package com.domisa.domisa_backend.notification.dto;

import com.domisa.domisa_backend.notification.type.NotificationType;
import java.util.List;

public record NotificationSimpleListResponse(List<NotificationItem> notifications) {

	public record NotificationItem(
		Long notificationId,
		String userId,
		NotificationType type
	) {
	}
}
