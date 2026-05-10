package com.domisa.domisa_backend.global.exception;

import com.domisa.domisa_backend.global.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception
	) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getDefaultMessage() == null ? "요청 값이 올바르지 않습니다." : error.getDefaultMessage())
			.orElse("요청 값이 올바르지 않습니다.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"INVALID_REQUEST",
				message
			));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(
		ConstraintViolationException exception
	) {
		String message = exception.getConstraintViolations().stream()
			.findFirst()
			.map(violation -> violation.getMessage() == null ? "요청 값이 올바르지 않습니다." : violation.getMessage())
			.orElse("요청 값이 올바르지 않습니다.");

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"INVALID_REQUEST",
				message
			));
	}
}
