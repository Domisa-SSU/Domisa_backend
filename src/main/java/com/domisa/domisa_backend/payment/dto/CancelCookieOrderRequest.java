package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CancelCookieOrderRequest(
	@JsonProperty("billing_name")
	@NotBlank(message = "billing_name은 필수입니다.")
	@Size(max = 30, message = "billing_name은 30자를 초과할 수 없습니다.")
	String billingName,

	@JsonProperty("order_amount")
	@NotNull(message = "order_amount는 필수입니다.")
	@Positive(message = "order_amount는 0보다 커야 합니다.")
	@Max(value = 10_000_000, message = "order_amount가 허용 범위를 초과했습니다.")
	Integer orderAmount
) {
}
