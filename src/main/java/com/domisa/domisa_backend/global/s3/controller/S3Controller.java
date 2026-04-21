package com.domisa.domisa_backend.global.s3.controller;

import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlResponse;
import com.domisa.domisa_backend.global.s3.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@Validated
@RestController
@RequestMapping("/api/v1/s3")
public class S3Controller {

	private final S3PresignedUrlService s3PresignedUrlService;

	public S3Controller(S3PresignedUrlService s3PresignedUrlService) {
		this.s3PresignedUrlService = s3PresignedUrlService;
	}

	@PostMapping("/presigned-urls/upload")
	@ResponseStatus(HttpStatus.CREATED)
	public GeneratePresignedUploadUrlResponse generatePresignedUploadUrl(
		@Valid @RequestBody GeneratePresignedUploadUrlRequest request
	) {
		return s3PresignedUrlService.generatePresignedUploadUrl(request);
	}
}
