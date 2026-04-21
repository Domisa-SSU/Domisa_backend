package com.domisa.domisa_backend.global.s3.exception;

import org.springframework.http.HttpStatus;

public enum S3ErrorCode {

	INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "S3_INVALID_FILE_NAME", "유효한 파일명이 아닙니다."),
	INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "S3_INVALID_CONTENT_TYPE", "유효한 contentType이 아닙니다."),
	INVALID_PREFIX(HttpStatus.BAD_REQUEST, "S3_INVALID_PREFIX", "유효한 prefix가 아닙니다."),
	PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_PRESIGNED_URL_GENERATION_FAILED", "Presigned URL 생성에 실패했습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "S3_INVALID_REQUEST", "요청 값이 올바르지 않습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	S3ErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
