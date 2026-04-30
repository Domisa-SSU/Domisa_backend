package com.domisa.domisa_backend.notification.dto;

import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.user.type.AnimalProfile;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResponse(List<NotificationItem> notifications) {

	public record NotificationItem(
		Long notificationId,
		String userId,
		NotificationType type,
		String targetUserId,
		AnimalProfile animalProfile,
		String personNickname,
		boolean isRead,
		LocalDateTime createdAt
	) {
	}
}
