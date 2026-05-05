package com.domisa.domisa_backend.dummy.controller;

import com.domisa.domisa_backend.dummy.dto.DummyLoginRequest;
import com.domisa.domisa_backend.dummy.dto.DummyLoginResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserCreateRequest;
import com.domisa.domisa_backend.dummy.dto.DummyUserCreateResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserListResponse;
import com.domisa.domisa_backend.dummy.service.DummyDataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
public class DummyController {

	private static final String DUMMY_ADMIN_KEY_HEADER = "X-Dummy-Admin-Key";

	private final DummyDataService dummyDataService;

	@PostMapping("/users")
	public ResponseEntity<DummyUserCreateResponse> createDummyUsers(
		@RequestHeader(value = DUMMY_ADMIN_KEY_HEADER, required = false) String adminKey,
		@RequestBody(required = false) DummyUserCreateRequest request
	) {
		return ResponseEntity.ok(dummyDataService.createDummyUsers(adminKey, request));
	}

	@GetMapping("/users")
	public ResponseEntity<DummyUserListResponse> getDummyUsers(
		@RequestHeader(value = DUMMY_ADMIN_KEY_HEADER, required = false) String adminKey
	) {
		return ResponseEntity.ok(dummyDataService.getDummyUsers(adminKey));
	}

	@PostMapping("/login")
	public ResponseEntity<DummyLoginResponse> loginAsDummyUser(
		@RequestHeader(value = DUMMY_ADMIN_KEY_HEADER, required = false) String adminKey,
		@RequestBody DummyLoginRequest request,
		HttpServletResponse response
	) {
		return ResponseEntity.ok(dummyDataService.login(adminKey, request, response));
	}
}
