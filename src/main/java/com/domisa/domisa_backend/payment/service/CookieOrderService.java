package com.domisa.domisa_backend.payment.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.payment.client.PayActionClient;
import com.domisa.domisa_backend.payment.dto.CancelCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CookieOrderPaymentStatusResponse;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionOrderExcludeRequest;
import com.domisa.domisa_backend.payment.entity.CookieCode;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class CookieOrderService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private static final DateTimeFormatter PAYACTION_ORDER_DATE_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	private final CookieOrderRepository cookieOrderRepository;
	private final PayActionClient payActionClient;
	private final CookieOrderTxService cookieOrderTxService;

	public CreateCookieOrderResponse createCookieOrder(Long userId, CreateCookieOrderRequest request) {
		CookieCode cookieCode = validateRequest(request);

		LocalDateTime orderDate = currentKoreaDateTime();
		CookieOrder order = cookieOrderTxService.createPendingOrder(userId, cookieCode, orderDate);

		PayActionCreateOrderRequest payActionRequest = PayActionCreateOrderRequest.requiredOnly(
				order.getOrderNumber(),
				order.getOrderAmount(),
				toIsoOffsetString(order.getOrderDate()),
				order.getBillingName(),
				order.getOrdererName()
		);

		try {
			PayActionCreateOrderResponse response = createPayActionOrder(payActionRequest);
			validatePayActionResponse(response);
		} catch (GlobalException exception) {
			cookieOrderTxService.markFailed(order.getId());
			throw exception;
		}

		return new CreateCookieOrderResponse(
				order.getBillingName(),
				order.getOrderAmount()
		);
	}

	@Transactional
	public void cancelCookieOrder(Long userId, CancelCookieOrderRequest request) {
		validateCancelRequest(request);

		CookieOrder order = cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmountWithLock(
						userId,
						request.billingName(),
						request.orderAmount()
				)
				.orElseThrow(() -> new GlobalException(GlobalErrorCode.COOKIE_ORDER_NOT_FOUND));

		if (order.getStatus() == OrderStatus.CANCELED) {
			return;
		}

		if (!order.isPending()) {
			throw new GlobalException(GlobalErrorCode.COOKIE_ORDER_INVALID_STATUS);
		}

		PayActionCreateOrderResponse payActionResponse = excludePayActionOrder(
				new PayActionOrderExcludeRequest(order.getOrderNumber())
		);

		validatePayActionExcludeResponse(payActionResponse);

		order.cancel();
	}

	@Transactional
	public void excludePendingOrdersForUser(Long userId) {
		List<CookieOrder> pendingOrders = cookieOrderRepository.findByUserIdAndStatusIn(
				userId,
				List.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PENDING)
		);

		for (CookieOrder order : pendingOrders) {
			PayActionCreateOrderResponse payActionResponse = excludePayActionOrder(
					new PayActionOrderExcludeRequest(order.getOrderNumber())
			);

			validatePayActionExcludeResponse(payActionResponse);

			order.cancel();
		}
	}

	@Transactional(readOnly = true)
	public CookieOrderPaymentStatusResponse getCookieOrderPaymentStatus(
			Long userId,
			String billingName,
			Integer orderAmount
	) {
		validatePaymentStatusRequest(billingName, orderAmount);

		return cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmount(
						userId,
						billingName,
						orderAmount
				)
				.map(order -> order.isPaid()
						? CookieOrderPaymentStatusResponse.paid(order.getCookieAmount())
						: CookieOrderPaymentStatusResponse.unconfirmed(order.getStatus().name()))
				.orElseGet(() -> CookieOrderPaymentStatusResponse.unconfirmed("UNCONFIRMED"));
	}

	LocalDateTime currentKoreaDateTime() {
		return LocalDateTime.now(KOREA_ZONE_ID).withNano(0);
	}

	private CookieCode validateRequest(CreateCookieOrderRequest request) {
		if (request == null || request.productCode() == null || request.productCode().isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}

		return CookieCode.fromProductCode(request.productCode());
	}

	private void validateCancelRequest(CancelCookieOrderRequest request) {
		if (request == null
				|| request.billingName() == null
				|| request.billingName().isBlank()
				|| request.orderAmount() == null
				|| request.orderAmount() <= 0) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
	}

	private void validatePaymentStatusRequest(String billingName, Integer orderAmount) {
		if (billingName == null || billingName.isBlank() || orderAmount == null || orderAmount <= 0) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
	}

	private String toIsoOffsetString(LocalDateTime localDateTime) {
		return OffsetDateTime.of(localDateTime, ZoneOffset.ofHours(9))
				.format(PAYACTION_ORDER_DATE_FORMATTER);
	}

	private PayActionCreateOrderResponse createPayActionOrder(PayActionCreateOrderRequest request) {
		try {
			return payActionClient.createOrder(request);
		} catch (RestClientException exception) {
			throw new GlobalException(
					GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED,
					"페이액션 주문 등록 실패: " + exception.getMessage()
			);
		}
	}

	private PayActionCreateOrderResponse excludePayActionOrder(PayActionOrderExcludeRequest request) {
		try {
			return payActionClient.excludeOrder(request);
		} catch (RestClientException exception) {
			throw new GlobalException(
					GlobalErrorCode.PAYACTION_ORDER_EXCLUDE_FAILED,
					"페이액션 주문 매칭 제외 실패: " + exception.getMessage()
			);
		}
	}

	private void validatePayActionResponse(PayActionCreateOrderResponse response) {
		if (response != null && response.isSuccess()) {
			return;
		}

		String message = response != null && response.response() != null && response.response().message() != null
				? response.response().message()
				: GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED.getMessage();

		throw new GlobalException(
				GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED,
				"페이액션 주문 등록 실패: " + message
		);
	}

	private void validatePayActionExcludeResponse(PayActionCreateOrderResponse response) {
		if (response != null && response.isSuccess()) {
			return;
		}

		String message = response != null && response.response() != null && response.response().message() != null
				? response.response().message()
				: GlobalErrorCode.PAYACTION_ORDER_EXCLUDE_FAILED.getMessage();

		throw new GlobalException(
				GlobalErrorCode.PAYACTION_ORDER_EXCLUDE_FAILED,
				"페이액션 주문 매칭 제외 실패: " + message
		);
	}
}