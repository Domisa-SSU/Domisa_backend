package com.domisa.domisa_backend.auth.controller;

import com.domisa.domisa_backend.auth.dto.LoginRequest;
import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(
		@RequestBody LoginRequest request,
		HttpServletResponse response
	) {
		LoginResponse loginResponse = authService.login(request.authorizationCode(), response);
		return ResponseEntity.ok(loginResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
		return ResponseEntity.ok(authService.logout(response));
	}
}
