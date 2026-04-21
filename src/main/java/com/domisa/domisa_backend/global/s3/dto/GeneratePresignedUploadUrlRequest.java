package com.domisa.domisa_backend.global.s3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneratePresignedUploadUrlRequest(
	@NotBlank(message = "contentType은 필수입니다.")
	@Size(max = 100, message = "contentType은 100자를 초과할 수 없습니다.")
	String contentType
) {
}
