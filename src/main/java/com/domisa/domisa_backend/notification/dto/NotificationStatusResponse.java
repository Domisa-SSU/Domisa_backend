package com.domisa.domisa_backend.notification.dto;

public record NotificationStatusResponse(
	boolean hasUnread,
	long unreadCount
) {
}
