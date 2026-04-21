package com.domisa.domisa_backend.global.s3.exception;

public class S3Exception extends RuntimeException {

	private final S3ErrorCode errorCode;

	public S3Exception(S3ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public S3Exception(S3ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}

	public S3Exception(S3ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public S3ErrorCode getErrorCode() {
		return errorCode;
	}
}
