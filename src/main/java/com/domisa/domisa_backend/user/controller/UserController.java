package com.domisa.domisa_backend.user.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.profile.dto.MyIntroductionResponse;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterResponse;
import com.domisa.domisa_backend.profile.dto.ProfileUpdateRequest;
import com.domisa.domisa_backend.profile.dto.ProfileUpdateResponse;
import com.domisa.domisa_backend.profile.service.ProfileService;
import com.domisa.domisa_backend.user.dto.UserCookiesResponse;
import com.domisa.domisa_backend.user.dto.UserLikesReceivedResponse;
import com.domisa.domisa_backend.user.dto.UserLikesSentResponse;
import com.domisa.domisa_backend.user.dto.UserMeResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final ProfileService profileService;

	// 닉네임 중복 조회
	@GetMapping("/check-nickname")
	public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
		return ResponseEntity.ok(profileService.checkNickname(nickname));
	}

	// 회원가입
	@PostMapping({"/register", "/profiles"})
	public ResponseEntity<ProfileRegisterResponse> registerProfile(
			@AuthUser User authUser,
			@RequestBody ProfileRegisterRequest request
	) {
		return ResponseEntity.ok(profileService.registerProfile(authUser.getId(), request));
	}

	// 내 정보 조회(마이페이지용)
	@GetMapping("/me")
	public ResponseEntity<UserMeResponse> getMe(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getMe(authUser));
	}

	@GetMapping("/introduction")
	public ResponseEntity<MyIntroductionResponse> getMyIntroduction(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getMyIntroduction(authUser));
	}

	@GetMapping("/cookies")
	public ResponseEntity<UserCookiesResponse> getMyCookies(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getMyCookies(authUser));
	}

	@GetMapping("/likes/received")
	public ResponseEntity<UserLikesReceivedResponse> getReceivedLikes(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getReceivedLikes(authUser));
	}

	@GetMapping("/likes/sent")
	public ResponseEntity<UserLikesSentResponse> getSentLikes(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getSentLikes(authUser));
	}

	// 나의 프로필 수정
	@PutMapping("/me")
	public ResponseEntity<ProfileUpdateResponse> updateResponse(
			@AuthUser User authUser,
			@RequestBody ProfileUpdateRequest request
	) {
		return ResponseEntity.ok(profileService.updateProfile(authUser.getId(), request));
	}
}
