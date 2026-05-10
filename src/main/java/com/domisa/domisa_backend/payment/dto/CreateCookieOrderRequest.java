package com.domisa.domisa_backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCookieOrderRequest(
		@JsonProperty("productCode")
		@NotBlank(message = "productCode는 필수입니다.")
		@Pattern(regexp = "COOKIE_(5|10|30|60)", message = "유효하지 않은 쿠키 상품입니다.")
		String productCode
) {

}
