package com.domisa.domisa_backend.global.s3.dto;

public record GeneratePresignedUploadUrlResponse(
	String objectKey,
	String presignedUrl
) {
}
