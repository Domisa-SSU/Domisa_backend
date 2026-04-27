package com.domisa.domisa_backend.global.s3.dto;

import java.time.Instant;

public record S3ErrorResponse(
	Instant timestamp,
	int status,
	String code,
	String message,
	String path
) {
}
