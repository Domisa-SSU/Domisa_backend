package com.domisa.domisa_backend.notification.controller;

import com.domisa.domisa_backend.notification.dto.NotificationListResponse;
import com.domisa.domisa_backend.notification.dto.NotificationReadResponse;
import com.domisa.domisa_backend.notification.dto.NotificationStatusResponse;
import com.domisa.domisa_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<NotificationListResponse> getNotifications(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(notificationService.getNotifications(userId));
	}

	@GetMapping("/status")
	public ResponseEntity<NotificationStatusResponse> getNotificationStatus(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(notificationService.getNotificationStatus(userId));
	}

	@PostMapping("/{notificationId}")
	public ResponseEntity<NotificationReadResponse> markAsRead(
		@AuthenticationPrincipal Long userId,
		@PathVariable Long notificationId
	) {
		return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));
	}
}
