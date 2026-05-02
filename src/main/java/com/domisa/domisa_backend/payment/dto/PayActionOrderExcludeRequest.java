package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PayActionOrderExcludeRequest(
	@JsonProperty("order_number") String orderNumber
) {
}
