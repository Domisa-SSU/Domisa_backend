package com.domisa.domisa_backend.notification.dto;

import com.domisa.domisa_backend.notification.type.NotificationType;
import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResponse(List<NotificationItem> notifications) {

	public record NotificationItem(
		Long notificationId,
		String userId,
		String targetUserId,
		NotificationType type,
		String title,
		String content,
		boolean isRead,
		LocalDateTime createdAt
	) {
	}
}
