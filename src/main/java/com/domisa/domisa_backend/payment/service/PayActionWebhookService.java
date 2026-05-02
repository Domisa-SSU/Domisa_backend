package com.domisa.domisa_backend.payment.service;

import com.domisa.domisa_backend.cookie.entity.CookieWallet;
import com.domisa.domisa_backend.cookie.repository.CookieWalletRepository;
import com.domisa.domisa_backend.payment.config.PayActionProperties;
import com.domisa.domisa_backend.payment.dto.PayActionMatchedWebhookRequest;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
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

		if (!"매칭완료".equals(request.orderStatus())) {
			throw new GlobalException(GlobalErrorCode.INVALID_PAYACTION_ORDER_STATUS);
		}

		CookieOrder order = cookieOrderRepository.findByOrderNumberWithLock(request.orderNumber())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.COOKIE_ORDER_NOT_FOUND));
		if (order.isPaid()) {
			return;
		}
		if (!order.isPending()) {
			throw new GlobalException(GlobalErrorCode.COOKIE_ORDER_INVALID_STATUS);
		}

		LocalDateTime processingDate = parseProcessingDate(request.processingDate());
		order.markPaid(processingDate);

		CookieWallet wallet = cookieWalletRepository.findByUserIdWithLock(order.getUser().getId())
			.orElseGet(() -> cookieWalletRepository.save(
				CookieWallet.create(order.getUser(), Math.toIntExact(order.getUser().getCookieBalance()))
			));
		wallet.add(order.getCookieAmount());
		order.getUser().addCookies(order.getCookieAmount());

		cookieTransactionRepository.save(CookieTransaction.charge(order.getUser(), order, order.getCookieAmount()));
	}

	private void validateWebhookHeaders(String webhookKey, String mallId) {
		if (!Objects.equals(payActionProperties.getWebhookKey(), webhookKey)) {
			throw new GlobalException(GlobalErrorCode.PAYACTION_WEBHOOK_UNAUTHORIZED);
		}
		if (!Objects.equals(payActionProperties.getMallId(), mallId)) {
			throw new GlobalException(GlobalErrorCode.PAYACTION_WEBHOOK_FORBIDDEN);
		}
	}

	private LocalDateTime parseProcessingDate(String processingDate) {
		if (processingDate == null || processingDate.isBlank()) {
			return null;
		}
		return OffsetDateTime.parse(processingDate).toLocalDateTime();
	}
}
