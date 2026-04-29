package com.domisa.domisa_backend.profile.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterResponse;
import com.domisa.domisa_backend.profile.dto.ProfileUpdateRequest;
import com.domisa.domisa_backend.profile.dto.ProfileUpdateResponse;
import com.domisa.domisa_backend.profile.service.ProfileService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;

	// 닉네임 중복 조회
	@GetMapping("/check-nickname")
	public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
		return ResponseEntity.ok(profileService.checkNickname(nickname));
	}

	// 회원가임
	@PostMapping("/register")
	public ResponseEntity<ProfileRegisterResponse> registerProfile(
		@AuthUser Long userId,
		@RequestBody ProfileRegisterRequest request
	) {
		return ResponseEntity.ok(profileService.registerProfile(userId, request));
	}

	// 나의 프로필 수정
	@PutMapping("/me")
	public ResponseEntity<ProfileUpdateResponse> updateResponse(
			@AuthUser Long userId,
			@RequestBody ProfileUpdateRequest request
			) {
		return ResponseEntity.ok(profileService.updateProfile(userId, request));
	}
}
