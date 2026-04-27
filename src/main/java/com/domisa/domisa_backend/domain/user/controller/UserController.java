package com.domisa.domisa_backend.domain.user.controller;

import com.domisa.domisa_backend.domain.user.dto.UserMeResponse;
import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.domain.user.repository.UserRepository;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserRepository userRepository;
	private final S3ObjectUrlService s3ObjectUrlService;

	public UserController(UserRepository userRepository, S3ObjectUrlService s3ObjectUrlService) {
		this.userRepository = userRepository;
		this.s3ObjectUrlService = s3ObjectUrlService;
	}

	@GetMapping("/me")
	public ResponseEntity<UserMeResponse> getMe(@AuthenticationPrincipal Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

		return ResponseEntity.ok(new UserMeResponse(
			new UserMeResponse.UserDto(
				user.getId(),
				user.getNickname(),
				user.getAge(),
				user.getGenderDisplay(),
				s3ObjectUrlService.getProfileImageUrl(user),
				Math.toIntExact(user.getCookies()),
				user.getInviteCode()
			),
			new UserMeResponse.StatusDto(
				user.getIsRegistered(),
				user.getHasIntroduction(),
				user.getIsProfileCompleted()
			)
		));
	}
}
