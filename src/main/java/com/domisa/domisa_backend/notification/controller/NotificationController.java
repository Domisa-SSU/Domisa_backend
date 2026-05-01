package com.domisa.domisa_backend.notification.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.notification.dto.NotificationActiveResponse;
import com.domisa.domisa_backend.notification.dto.NotificationListResponse;
import com.domisa.domisa_backend.notification.dto.NotificationStatusResponse;
import com.domisa.domisa_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<NotificationListResponse> getNotifications(@AuthUser Long userId) {
		return ResponseEntity.ok(notificationService.getNotifications(userId));
	}

	@GetMapping("/active")
	public ResponseEntity<NotificationActiveResponse> getActiveNotifications(@AuthUser Long userId) {
		return ResponseEntity.ok(notificationService.getActiveNotifications(userId));
	}

	@GetMapping("/status")
	public ResponseEntity<NotificationStatusResponse> getNotificationStatus(@AuthUser Long userId) {
		return ResponseEntity.ok(notificationService.getNotificationStatus(userId));
	}

	@PostMapping("/{notificationId}")
	public ResponseEntity<Void> markNotificationAsRead(
		@AuthUser Long userId,
		@PathVariable Long notificationId
	) {
		notificationService.markAsRead(userId, notificationId);
		return ResponseEntity.noContent().build();
	}
}
