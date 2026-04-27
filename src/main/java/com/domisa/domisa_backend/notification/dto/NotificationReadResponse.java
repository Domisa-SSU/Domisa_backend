package com.domisa.domisa_backend.notification.dto;

public record NotificationReadResponse(
	Long notificationId,
	boolean isRead
) {
}
