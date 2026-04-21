package com.domisa.domisa_backend.global.s3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteS3ObjectRequest(
	@NotBlank(message = "삭제할 objectKey는 필수입니다.")
	@Size(max = 1024, message = "objectKey는 1024자를 초과할 수 없습니다.")
	String objectKey
) {
}
