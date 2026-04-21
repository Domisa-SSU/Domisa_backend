package com.domisa.domisa_backend.global.s3.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum S3ErrorCode {

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
	INVALID_USER_NAME(HttpStatus.BAD_REQUEST, "S3_INVALID_USER_NAME", "유효한 사용자 이름이 아닙니다."),
	INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "S3_INVALID_CONTENT_TYPE", "유효한 contentType이 아닙니다."),
	INVALID_PREFIX(HttpStatus.BAD_REQUEST, "S3_INVALID_PREFIX", "유효한 prefix가 아닙니다."),
	INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST, "S3_INVALID_OBJECT_KEY", "유효한 objectKey가 아닙니다."),
	OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "S3_OBJECT_NOT_FOUND", "S3 객체를 찾을 수 없습니다."),
	PROFILE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_IMAGE_NOT_FOUND", "프로필 이미지가 존재하지 않습니다."),
	PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_PRESIGNED_URL_GENERATION_FAILED", "Presigned URL 생성에 실패했습니다."),
	OBJECT_URL_RESOLUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_OBJECT_URL_RESOLUTION_FAILED", "S3 객체 URL 생성에 실패했습니다."),
	OBJECT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_OBJECT_DELETE_FAILED", "S3 객체 삭제에 실패했습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "S3_INVALID_REQUEST", "요청 값이 올바르지 않습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	S3ErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
