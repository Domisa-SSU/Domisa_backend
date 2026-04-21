package com.domisa.domisa_backend.global.s3.controller;

import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.global.auth.annotation.AuthUser;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlResponse;
import com.domisa.domisa_backend.global.s3.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users/me/profile-image")
public class S3Controller {

	private final S3PresignedUrlService s3PresignedUrlService;

	public S3Controller(S3PresignedUrlService s3PresignedUrlService) {
		this.s3PresignedUrlService = s3PresignedUrlService;
	}

	@PostMapping("/presigned-url")
	@ResponseStatus(HttpStatus.CREATED)
	public GeneratePresignedUploadUrlResponse issueProfileImageUploadUrl(
		@AuthUser User authUser,
		@Valid @RequestBody GeneratePresignedUploadUrlRequest request
	) {
		return s3PresignedUrlService.issueProfileImageUploadUrl(authUser, request);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProfileImage(@AuthUser User authUser) {
		s3PresignedUrlService.deleteProfileImage(authUser);
	}
}
