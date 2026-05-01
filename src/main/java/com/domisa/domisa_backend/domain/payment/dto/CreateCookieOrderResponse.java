package com.domisa.domisa_backend.domain.payment.dto;

public record CreateCookieOrderResponse(
	String orderNumber,
	Integer orderAmount,
	String billingName,
	String bankName,
	String bankCode,
	String accountNumber,
	String accountHolder,
	String status
) {
}
