package com.domisa.domisa_backend.payment.dto;

public record CreateCookieOrderRequest(
	Integer cookieAmount,
	Integer orderAmount
) {
}
