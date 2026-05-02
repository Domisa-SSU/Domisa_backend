package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CookieOrderPaymentStatusResponse(
	Boolean confirmed,
	String status,
	@JsonProperty("cookie_amount") Integer cookieAmount
) {
	public static CookieOrderPaymentStatusResponse paid(Integer cookieAmount) {
		return new CookieOrderPaymentStatusResponse(true, "PAID", cookieAmount);
	}

	public static CookieOrderPaymentStatusResponse unconfirmed(String status) {
		return new CookieOrderPaymentStatusResponse(false, status, null);
	}
}
