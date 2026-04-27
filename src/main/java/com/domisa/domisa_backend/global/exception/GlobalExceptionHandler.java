package com.domisa.domisa_backend.global.exception;

import com.domisa.domisa_backend.global.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(GlobalException.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(GlobalException exception) {
		GlobalErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(new ErrorResponse(
				errorCode.getHttpStatus().value(),
				errorCode.getCode(),
				exception.getMessage()
			));
	}
}
