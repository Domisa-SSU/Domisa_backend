package com.domisa.domisa_backend.global.s3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteProfileImageUploadRequest(
	@NotBlank(message = "uploadKey는 필수입니다.")
	@Size(max = 1024, message = "uploadKey는 1024자를 초과할 수 없습니다.")
	String uploadKey
) {
}
