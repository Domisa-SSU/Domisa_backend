package com.domisa.domisa_backend.user.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.user.dto.DatingProfileResponse;
import com.domisa.domisa_backend.user.dto.DatingRefreshTimeResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datings")
@RequiredArgsConstructor
public class DatingController {

	private final UserService userService;

	@GetMapping("/profiles/{userId}")
	public ResponseEntity<DatingProfileResponse> getDatingProfile(
		@AuthUser User authUser,
		@PathVariable Long userId
	) {
		return ResponseEntity.ok(userService.getDatingProfile(authUser, userId));
	}

	@GetMapping("/refresh-time")
	public ResponseEntity<DatingRefreshTimeResponse> getRefreshTime(@AuthUser User authUser) {
		return ResponseEntity.ok(userService.getDatingRefreshTime(authUser));
	}
}
