package com.domisa.domisa_backend.payment.service;

import com.domisa.domisa_backend.cookie.entity.CookieWallet;
import com.domisa.domisa_backend.cookie.repository.CookieWalletRepository;
import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.config.PayActionProperties;
import com.domisa.domisa_backend.payment.dto.PayActionMatchedWebhookRequest;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.entity.PayActionWebhookLog;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.payment.repository.PayActionWebhookLogRepository;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayActionWebhookService {

	private final PayActionProperties payActionProperties;
	private final CookieOrderRepository cookieOrderRepository;
	private final CookieWalletRepository cookieWalletRepository;
	private final CookieTransactionRepository cookieTransactionRepository;
	private final PayActionWebhookLogRepository payActionWebhookLogRepository;
	private final NotificationService notificationService;

	@Transactional
	public void handleMatchedWebhook(
		String webhookKey,
		String mallId,
		String traceId,
		PayActionMatchedWebhookRequest request
	) {
		validateWebhookHeaders(webhookKey, mallId);
		log.info(
			"Received PayAction matched webhook traceId={}, orderNumber={}, orderStatus={}",
			traceId,
			request.orderNumber(),
			request.orderStatus()
		);
		validateTraceId(traceId);
		if (payActionWebhookLogRepository.existsByTraceId(traceId)) {
			return;
		}

		if (!"매칭완료".equals(request.orderStatus())) {
			throw new GlobalException(GlobalErrorCode.INVALID_PAYACTION_ORDER_STATUS);
		}

		LocalDateTime processingDate = parseProcessingDate(request.processingDate());
		CookieOrder order = cookieOrderRepository.findByOrderNumberWithLock(request.orderNumber())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.COOKIE_ORDER_NOT_FOUND));
		if (order.isPaid()) {
			saveWebhookLog(traceId, request, processingDate);
			return;
		}
		if (!order.isPending()) {
			throw new GlobalException(GlobalErrorCode.COOKIE_ORDER_INVALID_STATUS);
		}

		order.markPaid(processingDate);

		CookieWallet wallet = cookieWalletRepository.findByUserIdWithLock(order.getUser().getId())
			.orElseGet(() -> cookieWalletRepository.save(
				CookieWallet.create(order.getUser(), Math.toIntExact(order.getUser().getCookieBalance()))
			));
		wallet.add(order.getCookieAmount());
		order.getUser().addCookies(order.getCookieAmount());

		cookieTransactionRepository.save(CookieTransaction.charge(order.getUser(), order, order.getCookieAmount()));
		notificationService.createNotification(NotificationType.COOKIE_PAYMENT, order.getUser().getId(), null);
		saveWebhookLog(traceId, request, processingDate);
	}

	private void validateWebhookHeaders(String webhookKey, String mallId) {
		if (!Objects.equals(payActionProperties.getWebhookKey(), webhookKey)) {
			throw new GlobalException(GlobalErrorCode.PAYACTION_WEBHOOK_UNAUTHORIZED);
		}
		if (!Objects.equals(payActionProperties.getMallId(), mallId)) {
			throw new GlobalException(GlobalErrorCode.PAYACTION_WEBHOOK_FORBIDDEN);
		}
	}

	private void validateTraceId(String traceId) {
		if (traceId == null || traceId.isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD, "페이액션 traceId가 누락되었습니다.");
		}
	}

	private LocalDateTime parseProcessingDate(String processingDate) {
		if (processingDate == null || processingDate.isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD, "페이액션 처리일시가 누락되었습니다.");
		}
		return OffsetDateTime.parse(processingDate).toLocalDateTime();
	}

	private void saveWebhookLog(
		String traceId,
		PayActionMatchedWebhookRequest request,
		LocalDateTime processingDate
	) {
		payActionWebhookLogRepository.save(PayActionWebhookLog.create(
			traceId,
			request.orderNumber(),
			request.orderStatus(),
			processingDate,
			LocalDateTime.now()
		));
	}
}
