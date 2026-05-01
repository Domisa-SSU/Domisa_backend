package com.domisa.domisa_backend.global.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "유저를 찾을 수 없습니다."),
	USER_PUBLIC_ID_GENERATION_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"USER_PUBLIC_ID_GENERATION_FAILED",
		"유저 공개 식별자 생성에 실패했습니다."
	),
	INTRODUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "INTRODUCTION_NOT_FOUND", "소개서를 찾을 수 없습니다."),
	INTRODUCTION_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "INTRODUCTION_ALREADY_ACCEPTED", "이미 수락된 소개서입니다."),
	USER_ALREADY_HAS_INTRODUCTION(HttpStatus.CONFLICT, "USER_ALREADY_HAS_INTRODUCTION", "이미 수락한 소개서가 있습니다."),
	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
	MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_FIELD", "필수 입력 항목이 누락되었습니다."),
	INVALID_NICKNAME_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME_LENGTH", "닉네임은 최대 4글자까지 입력 가능합니다."),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
	CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "소개팅 카드를 찾을 수 없습니다."),
	CARD_ALREADY_EXISTS(HttpStatus.CONFLICT, "CARD_ALREADY_EXISTS", "이미 소개팅 카드가 존재합니다."),
	USER_NOT_REGISTERED(HttpStatus.FORBIDDEN, "USER_NOT_REGISTERED", "회원가입이 완료되지 않은 사용자입니다."),
	CANNOT_LIKE_SELF(HttpStatus.BAD_REQUEST, "CANNOT_LIKE_SELF", "자기 자신에게 호감을 보낼 수 없습니다."),
	ALREADY_LIKED(HttpStatus.CONFLICT, "ALREADY_LIKED", "이미 호감을 보낸 사용자입니다."),
	INSUFFICIENT_COOKIES(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_COOKIES", "쿠키가 부족합니다.");

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
