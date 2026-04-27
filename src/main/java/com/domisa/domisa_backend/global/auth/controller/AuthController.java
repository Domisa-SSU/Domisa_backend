package com.domisa.domisa_backend.global.auth.controller;

import com.domisa.domisa_backend.global.auth.dto.LoginRequest;
import com.domisa.domisa_backend.global.auth.dto.LoginResponse;
import com.domisa.domisa_backend.global.auth.service.AuthCookieManager;
import com.domisa.domisa_backend.global.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthCookieManager authCookieManager;

	public AuthController(AuthService authService, AuthCookieManager authCookieManager) {
		this.authService = authService;
		this.authCookieManager = authCookieManager;
	}

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
		authCookieManager.expireCookie(response, "accessToken");
		authCookieManager.expireCookie(response, "refreshToken");
		return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
	}
}
