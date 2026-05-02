package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PayActionMatchedWebhookRequest(
	@JsonProperty("order_number") String orderNumber,
	@JsonProperty("order_status") String orderStatus,
	@JsonProperty("processing_date") String processingDate
) {
}
