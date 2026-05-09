package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CookieOrderPaymentStatusResponse(
	String status,
	@JsonProperty("cookie_amount") Integer cookieAmount
) {
	private static final String PAID = "PAID";
	private static final String ALREADY_PROCESSED = "ALREADY_PROCESSED";

	public static CookieOrderPaymentStatusResponse paid(Integer cookieAmount) {
		return new CookieOrderPaymentStatusResponse(PAID, cookieAmount);
	}

	public static CookieOrderPaymentStatusResponse alreadyProcessed(Integer cookieAmount) {
		return new CookieOrderPaymentStatusResponse(ALREADY_PROCESSED, cookieAmount);
	}

	public static CookieOrderPaymentStatusResponse unconfirmed(String status) {
		return new CookieOrderPaymentStatusResponse(status, null);
	}
}
