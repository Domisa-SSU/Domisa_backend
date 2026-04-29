package com.domisa.domisa_backend.user.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.user.dto.UserLikesReceivedResponse;
import com.domisa.domisa_backend.user.dto.UserLikesSentResponse;
import com.domisa.domisa_backend.user.dto.UserMeResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<UserMeResponse> getMe(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getMe(authUser));
	}

	@GetMapping("/likes/received")
	public ResponseEntity<UserLikesReceivedResponse> getReceivedLikes(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getReceivedLikes(authUser));
	}

	@GetMapping("/likes/sent")
	public ResponseEntity<UserLikesSentResponse> getSentLikes(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getSentLikes(authUser));
	}
}
