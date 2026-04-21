package com.domisa.domisa_backend.global.s3.dto;

import java.time.Instant;
import java.util.Map;

public record GeneratePresignedUploadUrlResponse(
	String bucket,
	String objectKey,
	String presignedUrl,
	String httpMethod,
	String contentType,
	Instant expiresAt,
	Map<String, String> requiredHeaders
) {
}
