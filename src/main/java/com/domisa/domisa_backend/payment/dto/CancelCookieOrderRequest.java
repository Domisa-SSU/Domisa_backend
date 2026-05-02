package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CancelCookieOrderRequest(
	@JsonProperty("billing_name") String billingName,
	@JsonProperty("order_amount") Integer orderAmount
) {
}
