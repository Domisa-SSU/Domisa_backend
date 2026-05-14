package com.domisa.domisa_backend.dms.controller;

import com.domisa.domisa_backend.notification.service.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dms-room/sms")
@RequiredArgsConstructor
public class DmsSmsController {

	private final NotificationSmsService notificationSmsService;

	@PostMapping("/send-all")
	public ResponseEntity<String> sendAllUsersSms(@RequestParam("message") String message) {
		notificationSmsService.sendAllUsersSms(message);
		return ResponseEntity.ok("전체 문자 발송 요청 완료");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
	}
}
