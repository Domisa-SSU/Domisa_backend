package com.domisa.domisa_backend.payment.service;

import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.config.PayActionProperties;
import com.domisa.domisa_backend.payment.dto.PayActionMatchCompleteWebhookRequest;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.entity.PayActionWebhookLog;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.payment.repository.PayActionWebhookLogRepository;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayActionWebhookService {

	private final PayActionProperties payActionProperties;
	private final CookieOrderRepository cookieOrderRepository;
	private final CookieTransactionRepository cookieTransactionRepository;
	private final PayActionWebhookLogRepository payActionWebhookLogRepository;
	private final NotificationService notificationService;
	private final UserRepository userRepository;

	@Transactional
	public void handleMatchComplete(
		String webhookKey,
		String mallId,
		String traceId,
		PayActionMatchCompleteWebhookRequest request
	) {
		validateWebhookHeaders(webhookKey, mallId);
		validateRequest(request);
		log.info(
			"페이액션 매칭완료 웹훅 처리를 시작합니다. traceId={}, orderNumber={}, orderStatus={}",
			traceId,
			request.orderNumber(),
			request.orderStatus()
		);
		if (hasText(traceId) && payActionWebhookLogRepository.existsByTraceId(traceId)) {
			log.info("이미 처리된 페이액션 웹훅입니다. traceId={}, orderNumber={}", traceId, request.orderNumber());
			return;
		}

		if (!"매칭완료".equals(request.orderStatus())) {
			log.info("페이액션 웹훅 상태가 매칭완료가 아니어서 처리를 건너뜁니다. orderNumber={}, orderStatus={}",
				request.orderNumber(), request.orderStatus());
			return;
		}

		LocalDateTime processingDate = parseProcessingDate(request.processingDate());
		log.info("페이액션 웹훅 주문 조회를 시작합니다. orderNumber={}", request.orderNumber());
		CookieOrder order = cookieOrderRepository.findByOrderNumberWithLock(request.orderNumber())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.COOKIE_ORDER_NOT_FOUND));
		log.info("페이액션 웹훅 주문을 조회했습니다. orderNumber={}, orderStatus={}",
			order.getOrderNumber(), order.getStatus());
		if (order.isPaid()) {
			log.info("이미 결제 완료된 주문이어서 쿠키 충전을 건너뜁니다. orderNumber={}", order.getOrderNumber());
			saveWebhookLog(traceId, request, processingDate);
			return;
		}
		if (!order.isPending()) {
			log.info("결제대기 상태가 아닌 주문이어서 쿠키 충전을 건너뜁니다. orderNumber={}, orderStatus={}",
				order.getOrderNumber(), order.getStatus());
			saveWebhookLog(traceId, request, processingDate);
			return;
		}

		log.info("페이액션 매칭완료 주문을 결제완료 처리합니다. orderNumber={}, cookieAmount={}",
			order.getOrderNumber(), order.getCookieAmount());
		order.markPaid(processingDate);
		User user = userRepository.findByIdWithLock(order.getUser().getId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
		user.addCookies(order.getCookieAmount());

		cookieTransactionRepository.save(CookieTransaction.charge(user, order, order.getCookieAmount()));
		notificationService.createNotification(NotificationType.COOKIE_PAYMENT, user.getId(), null);
		saveWebhookLog(traceId, request, processingDate);
		log.info("페이액션 매칭완료 웹훅 처리가 완료되었습니다. orderNumber={}, userId={}, cookieAmount={}",
			order.getOrderNumber(), user.getId(), order.getCookieAmount());
	}

	private void validateWebhookHeaders(String webhookKey, String mallId) {
		if (!Objects.equals(payActionProperties.getWebhookKey(), webhookKey)) {
			log.warn("페이액션 웹훅 인증키 검증에 실패했습니다.");
			throw new GlobalException(GlobalErrorCode.PAYACTION_WEBHOOK_UNAUTHORIZED);
		}
		if (!Objects.equals(payActionProperties.getMallId(), mallId)) {
			log.warn("페이액션 웹훅 상점 ID 검증에 실패했습니다.");
			throw new GlobalException(GlobalErrorCode.PAYACTION_WEBHOOK_FORBIDDEN);
		}
		log.info("페이액션 웹훅 헤더 검증이 완료되었습니다.");
	}

	private void validateRequest(PayActionMatchCompleteWebhookRequest request) {
		if (request == null
			|| request.orderNumber() == null
			|| request.orderNumber().isBlank()
			|| request.orderStatus() == null
			|| request.orderStatus().isBlank()
			|| request.processingDate() == null
			|| request.processingDate().isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
	}

	private LocalDateTime parseProcessingDate(String processingDate) {
		if (processingDate == null || processingDate.isBlank()) {
			log.warn("페이액션 웹훅 처리일시가 누락되었습니다.");
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD, "페이액션 처리일시가 누락되었습니다.");
		}
		try {
			return OffsetDateTime.parse(processingDate).toLocalDateTime();
		} catch (DateTimeParseException exception) {
			log.warn("페이액션 웹훅 처리일시 형식이 올바르지 않습니다. processingDate={}", processingDate);
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD, "페이액션 처리일시 형식이 올바르지 않습니다.");
		}
	}

	private void saveWebhookLog(
		String traceId,
		PayActionMatchCompleteWebhookRequest request,
		LocalDateTime processingDate
	) {
		if (!hasText(traceId)) {
			log.info("페이액션 웹훅 traceId가 없어 웹훅 로그 저장을 건너뜁니다. orderNumber={}", request.orderNumber());
			return;
		}
		try {
			payActionWebhookLogRepository.saveAndFlush(PayActionWebhookLog.create(
				traceId,
				request.orderNumber(),
				request.orderStatus(),
				processingDate,
				LocalDateTime.now()
			));
			log.info("페이액션 웹훅 로그를 저장했습니다. traceId={}, orderNumber={}", traceId, request.orderNumber());
		} catch (DataIntegrityViolationException exception) {
			log.info("중복 페이액션 웹훅입니다. traceId={}", traceId);
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
