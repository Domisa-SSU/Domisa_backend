package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PayActionCreateOrderRequest(
	@JsonProperty("order_number") String orderNumber,
	@JsonProperty("order_amount") Integer orderAmount,
	@JsonProperty("order_date") String orderDate,
	@JsonProperty("billing_name") String billingName,
	@JsonProperty("orderer_name") String ordererName
) {
	public static PayActionCreateOrderRequest requiredOnly(
		String orderNumber,
		Integer orderAmount,
		String orderDate,
		String billingName,
		String ordererName
	) {
		return new PayActionCreateOrderRequest(
			orderNumber,
			orderAmount,
			orderDate,
			billingName,
			ordererName
		);
	}
}
