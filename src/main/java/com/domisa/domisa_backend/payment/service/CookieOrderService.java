package com.domisa.domisa_backend.payment.service;

import com.domisa.domisa_backend.payment.client.PayActionClient;
import com.domisa.domisa_backend.payment.dto.CancelCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CookieOrderPaymentStatusResponse;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionOrderExcludeRequest;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class CookieOrderService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter PAYACTION_ORDER_DATE_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
	private static final int MAX_ORDER_SAVE_ATTEMPTS = 20;
	private static final int MAX_BILLING_NAME_ATTEMPTS = 20;

	private final UserRepository userRepository;
	private final CookieOrderRepository cookieOrderRepository;
	private final PayActionClient payActionClient;
	private final BillingNameGenerator billingNameGenerator;
	private final OrderNumberGenerator orderNumberGenerator;

	@Transactional(noRollbackFor = GlobalException.class)
	public CreateCookieOrderResponse createCookieOrder(Long userId, CreateCookieOrderRequest request) {
		validateRequest(request);

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
		LocalDateTime orderDate = currentKoreaDateTime();
		CookieOrder order = createPendingOrder(user, request, orderDate);
		PayActionCreateOrderRequest payActionRequest = PayActionCreateOrderRequest.requiredOnly(
			order.getOrderNumber(),
			order.getOrderAmount(),
			toIsoOffsetString(orderDate),
			order.getBillingName(),
			resolveOrdererName(user)
		);
		try {
			PayActionCreateOrderResponse payActionResponse = createPayActionOrder(payActionRequest);
			validatePayActionResponse(payActionResponse);
		} catch (GlobalException exception) {
			order.markFailed();
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

	String nextBillingNameCandidate() {
		return billingNameGenerator.generate();
	}

	private void validateRequest(CreateCookieOrderRequest request) {
		if (request == null || request.cookieAmount() == null || request.orderAmount() == null) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
		if (request.cookieAmount() <= 0 || request.orderAmount() <= 0) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
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

	private CookieOrder createPendingOrder(User user, CreateCookieOrderRequest request, LocalDateTime orderDate) {
		for (int attempt = 0; attempt < MAX_ORDER_SAVE_ATTEMPTS; attempt++) {
			String orderNumber = nextOrderNumber(orderDate.toLocalDate());
			String billingName = generateUniqueBillingName(request.orderAmount());
			CookieOrder order = CookieOrder.create(
				user,
				orderNumber,
				billingName,
				request.orderAmount(),
				request.cookieAmount(),
				resolveOrdererName(user),
				orderDate
			);
			try {
				return cookieOrderRepository.saveAndFlush(order);
			} catch (DataIntegrityViolationException exception) {
				if (isOrderNumberCollision(exception)) {
					continue;
				}
				throw exception;
			}
		}

		throw new GlobalException(GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED, "쿠키 주문 번호 생성에 실패했습니다.");
	}

	private String nextOrderNumber(LocalDate orderDate) {
		String prefix = orderNumberGenerator.prefix(orderDate);
		int nextSequence = cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(prefix)
			.map(CookieOrder::getOrderNumber)
			.map(orderNumber -> Integer.parseInt(orderNumber.substring(prefix.length())) + 1)
			.orElse(1);
		return orderNumberGenerator.generate(orderDate, nextSequence);
	}

	private String generateUniqueBillingName(Integer orderAmount) {
		for (int attempt = 0; attempt < MAX_BILLING_NAME_ATTEMPTS; attempt++) {
			String billingName = nextBillingNameCandidate();
			if (!cookieOrderRepository.existsByBillingNameAndStatus(billingName, OrderStatus.PAYMENT_PENDING)
				&& !cookieOrderRepository.existsByBillingNameAndStatus(billingName, OrderStatus.PENDING)) {
				return billingName;
			}
		}

		throw new GlobalException(GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED, "입금자명 생성에 실패했습니다.");
	}

	private String resolveOrdererName(User user) {
		if (user.getName() != null && !user.getName().isBlank()) {
			return user.getName();
		}
		if (user.getNickname() != null && !user.getNickname().isBlank()) {
			return user.getNickname();
		}
		return user.getPublicId();
	}

	private String toIsoOffsetString(LocalDateTime localDateTime) {
		return OffsetDateTime.of(localDateTime, ZoneOffset.ofHours(9)).format(PAYACTION_ORDER_DATE_FORMATTER);
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
		throw new GlobalException(GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED, "페이액션 주문 등록 실패: " + message);
	}

	private void validatePayActionExcludeResponse(PayActionCreateOrderResponse response) {
		if (response != null && response.isSuccess()) {
			return;
		}

		String message = response != null && response.response() != null && response.response().message() != null
			? response.response().message()
			: GlobalErrorCode.PAYACTION_ORDER_EXCLUDE_FAILED.getMessage();
		throw new GlobalException(GlobalErrorCode.PAYACTION_ORDER_EXCLUDE_FAILED, "페이액션 주문 매칭 제외 실패: " + message);
	}

	private boolean isOrderNumberCollision(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			String message = current.getMessage();
			if (message != null) {
				String normalized = message.toLowerCase();
				if (normalized.contains("order_number") || normalized.contains("cookie_orders")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
