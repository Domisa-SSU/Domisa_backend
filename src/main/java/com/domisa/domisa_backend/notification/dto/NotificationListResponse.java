package com.domisa.domisa_backend.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.domisa.domisa_backend.notification.type.NotificationType;
import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResponse(List<NotificationItem> notifications) {

	public record NotificationItem(
		@JsonProperty("notification_id")
		Long notificationId,
		Long userId,
		NotificationType type,
		String title,
		String content,
		boolean isRead,
		LocalDateTime createdAt
	) {
	}
}
