package com.domisa.domisa_backend.global.exception;

public class GlobalException extends RuntimeException {

	private final GlobalErrorCode errorCode;

	public GlobalException(GlobalErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public GlobalErrorCode getErrorCode() {
		return errorCode;
	}
}
