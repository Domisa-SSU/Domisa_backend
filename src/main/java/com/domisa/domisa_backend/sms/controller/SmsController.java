package com.domisa.domisa_backend.sms.controller;

import com.domisa.domisa_backend.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SmsController {

	private final SmsService smsService;

	@PostMapping("/sms")
	public ResponseEntity<Void> send(
		@RequestParam("phone") String phone,
		@RequestParam("message") String message
	) {
		smsService.send(phone, message);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
