package com.domisa.domisa_backend.domain.payment.dto;

public record PayActionWebhookResponse(
	String status
) {
	public static PayActionWebhookResponse success() {
		return new PayActionWebhookResponse("success");
	}
}
