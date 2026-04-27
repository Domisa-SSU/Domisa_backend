package com.domisa.domisa_backend.global.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "유저를 찾을 수 없습니다."),
	MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_FIELD", "필수 입력 항목이 누락되었습니다."),
	INVALID_NICKNAME_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME_LENGTH", "닉네임은 최대 4글자까지 입력 가능합니다."),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	GlobalErrorCode(HttpStatus httpStatus, String code, String message) {
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
