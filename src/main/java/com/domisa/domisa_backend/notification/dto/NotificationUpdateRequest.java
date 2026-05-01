package com.domisa.domisa_backend.notification.dto;

public record NotificationUpdateRequest(
	Long notificationId,
	boolean isRead,
	boolean isClosed
) {
}
