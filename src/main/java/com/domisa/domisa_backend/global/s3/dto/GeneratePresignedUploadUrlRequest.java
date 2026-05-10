package com.domisa.domisa_backend.global.s3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GeneratePresignedUploadUrlRequest(
	@NotBlank(message = "contentType은 필수입니다.")
	@Size(max = 100, message = "contentType은 100자를 초과할 수 없습니다.")
	String contentType,

	@NotNull(message = "fileSize는 필수입니다.")
	@Positive(message = "fileSize는 0보다 커야 합니다.")
	@Max(value = 10_485_760, message = "fileSize는 10MB를 초과할 수 없습니다.")
	Long fileSize
) {
}
