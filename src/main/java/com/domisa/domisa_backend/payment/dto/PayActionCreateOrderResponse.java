package com.domisa.domisa_backend.payment.dto;

public record PayActionCreateOrderResponse(
	String status,
	PayActionResponseBody response
) {
	public boolean isSuccess() {
		return "success".equalsIgnoreCase(status);
	}
}
