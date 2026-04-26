package com.domisa.domisa_backend.domain.user.controller;

import com.domisa.domisa_backend.domain.user.dto.UserMeResponse;
import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.domain.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserRepository userRepository;

	public UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
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
				user.getProfileImageUrl(),
				user.getCookieCount(),
				user.getReferralCode()
			),
			new UserMeResponse.StatusDto(
				user.getIsRegistered(),
				user.getHasIntroduction(),
				user.getIsProfileCompleted()
			)
		));
	}
}
