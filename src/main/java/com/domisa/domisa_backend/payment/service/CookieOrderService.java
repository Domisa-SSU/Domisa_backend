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
import com.domisa.domisa_backend.user.entity.User;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieOrderService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private static final DateTimeFormatter PAYACTION_ORDER_DATE_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	private final CookieOrderRepository cookieOrderRepository;
	private final PayActionClient payActionClient;
	private final CookieOrderTxService cookieOrderTxService;

	public CreateCookieOrderResponse createCookieOrder(User user, CreateCookieOrderRequest request) {
		CookieCode cookieCode = validateRequest(request);
		log.info("쿠키 주문 생성을 시작합니다. userId={}, productCode={}", user.getId(), cookieCode.name());

		LocalDateTime orderDate = currentKoreaDateTime();
		CookieOrder order = cookieOrderTxService.createPendingOrder(user.getId(), cookieCode, orderDate);
		log.info("쿠키 결제대기 주문을 생성했습니다. userId={}, orderNumber={}, orderAmount={}, cookieAmount={}",
				user.getId(), order.getOrderNumber(), order.getOrderAmount(), order.getCookieAmount());

		PayActionCreateOrderRequest payActionRequest = PayActionCreateOrderRequest.requiredOnly(
				order.getOrderNumber(),
				order.getOrderAmount(),
				toIsoOffsetString(order.getOrderDate()),
				order.getBillingName(),
				order.getOrdererName()
		);

		try {
			log.info("페이액션 주문 등록을 요청합니다. orderNumber={}", order.getOrderNumber());
			PayActionCreateOrderResponse response = createPayActionOrder(payActionRequest);
			validatePayActionResponse(response);
			log.info("페이액션 주문 등록이 완료되었습니다. orderNumber={}", order.getOrderNumber());
		} catch (GlobalException exception) {
			log.warn("페이액션 주문 등록에 실패하여 주문을 실패 처리합니다. orderNumber={}, code={}",
					order.getOrderNumber(), exception.getErrorCode().getCode());
			cookieOrderTxService.markFailed(order.getId());
			throw exception;
		}

		log.info("쿠키 주문 생성 응답을 반환합니다. orderNumber={}, billingName={}, orderAmount={}",
				order.getOrderNumber(), order.getBillingName(), order.getOrderAmount());
		return new CreateCookieOrderResponse(
				order.getBillingName(),
				order.getOrderAmount()
		);
	}

	@Transactional
	public void cancelCookieOrder(User user, CancelCookieOrderRequest request) {
		validateCancelRequest(request);
		log.info("쿠키 주문 취소 요청을 수신했습니다. userId={}, billingName={}, orderAmount={}",
				user.getId(), request.billingName(), request.orderAmount());

		CookieOrder order = cookieOrderRepository.findAllByUserIdAndBillingNameAndOrderAmountWithLock(
						user.getId(),
						request.billingName(),
						request.orderAmount()
				)
				.stream()
				.findFirst()
				.orElseThrow(() -> new GlobalException(GlobalErrorCode.COOKIE_ORDER_NOT_FOUND));

		if (order.getStatus() == OrderStatus.CANCELED) {
			log.info("이미 취소된 쿠키 주문입니다. orderNumber={}", order.getOrderNumber());
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
		log.info("쿠키 주문 취소가 완료되었습니다. orderNumber={}", order.getOrderNumber());
	}

	@Transactional
	public void excludePendingOrdersForUser(User user) {
		List<CookieOrder> pendingOrders = cookieOrderRepository.findByUserIdAndStatusIn(
				user.getId(),
				List.of(OrderStatus.PENDING)
		);
		log.info("사용자의 결제대기 주문 매칭 제외를 시작합니다. userId={}, pendingOrderCount={}",
				user.getId(), pendingOrders.size());

		for (CookieOrder order : pendingOrders) {
			log.info("페이액션 주문 매칭 제외를 요청합니다. orderNumber={}", order.getOrderNumber());
			PayActionCreateOrderResponse payActionResponse = excludePayActionOrder(
					new PayActionOrderExcludeRequest(order.getOrderNumber())
			);

			validatePayActionExcludeResponse(payActionResponse);

			order.cancel();
			log.info("결제대기 주문 매칭 제외 및 취소가 완료되었습니다. orderNumber={}", order.getOrderNumber());
		}
	}

	public void excludePayActionOrderByOrderNumber(String orderNumber) {
		if (orderNumber == null || orderNumber.isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
		log.info("페이액션 주문 매칭 제외를 요청합니다. orderNumber={}", orderNumber);
		PayActionCreateOrderResponse payActionResponse = excludePayActionOrder(
			new PayActionOrderExcludeRequest(orderNumber)
		);
		validatePayActionExcludeResponse(payActionResponse);
		log.info("페이액션 주문 매칭 제외가 완료되었습니다. orderNumber={}", orderNumber);
	}

	@Transactional
	public CookieOrderPaymentStatusResponse getCookieOrderPaymentStatus(
			User user,
			String billingName,
			Integer orderAmount
	) {
		validatePaymentStatusRequest(billingName, orderAmount);
		log.info("쿠키 주문 결제 상태 조회를 시작합니다. userId={}, billingName={}, orderAmount={}",
				user.getId(), billingName, orderAmount);

		return cookieOrderRepository.findAllByUserIdAndBillingNameAndOrderAmountWithLock(
						user.getId(),
						billingName,
						orderAmount
				)
				.stream()
				.findFirst()
				.map(order -> {
					log.info("쿠키 주문 결제 상태를 조회했습니다. userId={}, orderNumber={}, orderStatus={}, isPaid={}",
							user.getId(), order.getOrderNumber(), order.getStatus(), order.isPaid());
					if (order.isAlreadyProcessed()) {
						log.info("이미 처리된 쿠키 지급 완료 주문입니다. userId={}, orderNumber={}",
								user.getId(), order.getOrderNumber());
						return CookieOrderPaymentStatusResponse.alreadyProcessed(order.getCookieAmount());
					}
					if (order.getStatus() == OrderStatus.FAILED) {
						log.info("실패한 쿠키 주문은 미확인 상태로 응답합니다. userId={}, orderNumber={}",
								user.getId(), order.getOrderNumber());
						return CookieOrderPaymentStatusResponse.unconfirmed("UNCONFIRMED");
					}
					if (!order.isPaymentCompleted()) {
						return CookieOrderPaymentStatusResponse.unconfirmed(order.getStatus().name());
					}

					order.markAlreadyProcessed();
					log.info("쿠키 지급 완료를 응답하고 이미 처리 상태로 변경했습니다. userId={}, orderNumber={}, cookieAmount={}",
							user.getId(), order.getOrderNumber(), order.getCookieAmount());
					return CookieOrderPaymentStatusResponse.paid(order.getCookieAmount());
				})
				.orElseGet(() -> {
					log.info("쿠키 주문 결제 상태 조회 결과가 없습니다. userId={}, billingName={}, orderAmount={}",
							user.getId(), billingName, orderAmount);
					return CookieOrderPaymentStatusResponse.unconfirmed("UNCONFIRMED");
				});
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
			log.warn("페이액션 주문 등록 API 호출에 실패했습니다. orderNumber={}, message={}",
					request.orderNumber(), exception.getMessage());
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
			log.warn("페이액션 주문 매칭 제외 API 호출에 실패했습니다. orderNumber={}, message={}",
					request.orderNumber(), exception.getMessage());
			throw new GlobalException(
					GlobalErrorCode.PAYACTION_ORDER_EXCLUDE_FAILED,
					"페이액션 주문 매칭 제외 실패: " + exception.getMessage()
			);
		}
	}

	private void validatePayActionResponse(PayActionCreateOrderResponse response) {
		if (response != null && response.isSuccess()) {
			log.info("페이액션 주문 등록 응답이 성공입니다.");
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
			log.info("페이액션 주문 매칭 제외 응답이 성공입니다.");
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
