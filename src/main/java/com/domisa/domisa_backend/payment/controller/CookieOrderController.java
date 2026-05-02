package com.domisa.domisa_backend.payment.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.payment.dto.CancelCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CookieOrderPaymentStatusResponse;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.payment.service.CookieOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@PostMapping("/cancel")
	public ResponseEntity<Void> cancelCookieOrder(
		@AuthUser Long userId,
		@RequestBody CancelCookieOrderRequest request
	) {
		cookieOrderService.cancelCookieOrder(userId, request);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@GetMapping("/status")
	public ResponseEntity<CookieOrderPaymentStatusResponse> getCookieOrderPaymentStatus(
		@AuthUser Long userId,
		@RequestParam("billing_name") String billingName,
		@RequestParam("order_amount") Integer orderAmount
	) {
		return ResponseEntity.ok(cookieOrderService.getCookieOrderPaymentStatus(userId, billingName, orderAmount));
	}
}
