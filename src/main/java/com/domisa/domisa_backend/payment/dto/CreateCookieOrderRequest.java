package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateCookieOrderRequest(
		@JsonProperty("productCode") String productCode
) {

}
