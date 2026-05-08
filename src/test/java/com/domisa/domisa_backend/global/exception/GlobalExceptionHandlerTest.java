package com.domisa.domisa_backend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.domisa.domisa_backend.global.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleGlobalExceptionReturnsInsufficientCookiesResponse() {
		ResponseEntity<ErrorResponse> response = handler.handleGlobalException(
			new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES)
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
		assertThat(response.getBody()).isEqualTo(new ErrorResponse(
			402,
			"INSUFFICIENT_COOKIES",
			"쿠키가 부족합니다."
		));
	}
}
