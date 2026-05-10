package com.domisa.domisa_backend.payment.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.payment.dto.CancelCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CookieOrderPaymentStatusResponse;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.payment.service.CookieOrderService;
import com.domisa.domisa_backend.user.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/orders/cookies")
@RequiredArgsConstructor
public class CookieOrderController {

	private final CookieOrderService cookieOrderService;

	@PostMapping
	public ResponseEntity<CreateCookieOrderResponse> createCookieOrder(
		@AuthUser User user,
		@Valid @RequestBody CreateCookieOrderRequest request
	) {
		return ResponseEntity.ok(cookieOrderService.createCookieOrder(user, request));
	}

	@PostMapping("/cancel")
	public ResponseEntity<Void> cancelCookieOrder(
		@AuthUser User user,
		@Valid @RequestBody CancelCookieOrderRequest request
	) {
		cookieOrderService.cancelCookieOrder(user, request);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@GetMapping("/status")
	public ResponseEntity<CookieOrderPaymentStatusResponse> getCookieOrderPaymentStatus(
		@AuthUser User user,
		@RequestParam("billing_name")
		@NotBlank(message = "billing_name은 필수입니다.")
		@Size(max = 30, message = "billing_name은 30자를 초과할 수 없습니다.")
		String billingName,
		@RequestParam("order_amount")
		@Positive(message = "order_amount는 0보다 커야 합니다.")
		@Max(value = 10_000_000, message = "order_amount가 허용 범위를 초과했습니다.")
		Integer orderAmount
	) {
		return ResponseEntity.ok(cookieOrderService.getCookieOrderPaymentStatus(user, billingName, orderAmount));
	}
}
