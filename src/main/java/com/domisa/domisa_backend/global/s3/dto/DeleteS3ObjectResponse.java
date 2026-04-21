package com.domisa.domisa_backend.global.s3.dto;

public record DeleteS3ObjectResponse(
	String objectKey,
	boolean deleted
) {
}
