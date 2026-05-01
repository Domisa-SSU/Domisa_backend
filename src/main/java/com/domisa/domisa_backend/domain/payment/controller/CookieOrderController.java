package com.domisa.domisa_backend.domain.payment.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.domain.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.domain.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.domain.payment.service.CookieOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/cookies")
@RequiredArgsConstructor
public class CookieOrderController {

	private final CookieOrderService cookieOrderService;

	@PostMapping
	public ResponseEntity<CreateCookieOrderResponse> createCookieOrder(
		@AuthUser Long userId,
		@RequestBody CreateCookieOrderRequest request
	) {
		return ResponseEntity.ok(cookieOrderService.createCookieOrder(userId, request));
	}
}
