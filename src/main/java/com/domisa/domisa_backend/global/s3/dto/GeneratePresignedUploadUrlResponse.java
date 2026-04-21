package com.domisa.domisa_backend.global.s3.dto;

import java.time.Instant;
import java.util.Map;

public record GeneratePresignedUploadUrlResponse(
	Long userId,
	String profileImageUrl,
	String bucket,
	String objectKey,
	String presignedUrl,
	String httpMethod,
	String contentType,
	Long uploadSequence,
	Instant expiresAt,
	Map<String, String> requiredHeaders
) {
}
