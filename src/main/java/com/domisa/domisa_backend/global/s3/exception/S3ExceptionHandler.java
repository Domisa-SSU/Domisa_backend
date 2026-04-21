package com.domisa.domisa_backend.global.s3.exception;

import com.domisa.domisa_backend.global.s3.controller.S3Controller;
import com.domisa.domisa_backend.global.s3.dto.S3ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = S3Controller.class)
public class S3ExceptionHandler {

	@ExceptionHandler(S3Exception.class)
	public ResponseEntity<S3ErrorResponse> handleS3Exception(S3Exception exception, HttpServletRequest request) {
		S3ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(new S3ErrorResponse(
				Instant.now(),
				errorCode.getHttpStatus().value(),
				errorCode.getCode(),
				exception.getMessage(),
				request.getRequestURI()
			));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
	public ResponseEntity<S3ErrorResponse> handleValidationException(Exception exception, HttpServletRequest request) {
		String message = extractValidationMessage(exception);
		S3ErrorCode errorCode = S3ErrorCode.INVALID_REQUEST;
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(new S3ErrorResponse(
				Instant.now(),
				errorCode.getHttpStatus().value(),
				errorCode.getCode(),
				message,
				request.getRequestURI()
			));
	}

	private String extractValidationMessage(Exception exception) {
		if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
			return methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		}
		if (exception instanceof BindException bindException) {
			return bindException.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		}
		if (exception instanceof ConstraintViolationException constraintViolationException) {
			return constraintViolationException.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.collect(Collectors.joining(", "));
		}
		return S3ErrorCode.INVALID_REQUEST.getMessage();
	}
}
