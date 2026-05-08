package com.domisa.domisa_backend.payment.controller;

import com.domisa.domisa_backend.payment.dto.PayActionMatchCompleteWebhookRequest;
import com.domisa.domisa_backend.payment.dto.PayActionWebhookResponse;
import com.domisa.domisa_backend.payment.service.PayActionWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/payaction")
@RequiredArgsConstructor
public class PayActionWebhookController {

	private final PayActionWebhookService payActionWebhookService;

	@PostMapping("/match-complete")
	public ResponseEntity<PayActionWebhookResponse> receiveMatchCompleteWebhook(
		@RequestHeader("x-webhook-key") String webhookKey,
		@RequestHeader("x-mall-id") String mallId,
		@RequestHeader(value = "x-trace-id", required = false) String traceId,
		@RequestBody PayActionMatchCompleteWebhookRequest request
	) {
		payActionWebhookService.handleMatchComplete(webhookKey, mallId, traceId, request);
		return ResponseEntity.ok(PayActionWebhookResponse.success());
	}
}
