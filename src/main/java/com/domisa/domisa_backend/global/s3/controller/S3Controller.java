package com.domisa.domisa_backend.global.s3.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.global.s3.dto.CompleteProfileImageUploadRequest;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlResponse;
import com.domisa.domisa_backend.global.s3.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("/api/users/me/profile-image")
public class S3Controller {

	private final S3PresignedUrlService s3PresignedUrlService;

	@PostMapping("/upload-url")
	@ResponseStatus(HttpStatus.CREATED)
	public GeneratePresignedUploadUrlResponse issueProfileImageUploadUrl(
		@AuthUser User authUser,
		@Valid @RequestBody GeneratePresignedUploadUrlRequest request
	) {
		return s3PresignedUrlService.issueProfileImageUploadUrl(authUser, request);
	}

	@PostMapping("/complete")
	@ResponseStatus(HttpStatus.OK)
	public void completeProfileImageUpload(
		@AuthUser User authUser,
		@Valid @RequestBody CompleteProfileImageUploadRequest request
	) {
		s3PresignedUrlService.completeProfileImageUpload(authUser, request);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProfileImage(@AuthUser User authUser) {
		s3PresignedUrlService.deleteProfileImage(authUser);
	}
}
