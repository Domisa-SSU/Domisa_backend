package com.domisa.domisa_backend.notification.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.notification.dto.NotificationListResponse;
import com.domisa.domisa_backend.notification.dto.NotificationReadResponse;
import com.domisa.domisa_backend.notification.dto.NotificationStatusResponse;
import com.domisa.domisa_backend.notification.entity.Notification;
import com.domisa.domisa_backend.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;

	@Transactional(readOnly = true)
	public NotificationListResponse getNotifications(Long userId) {
		List<NotificationListResponse.NotificationItem> notifications = notificationRepository
			.findAllByUserIdOrderByCreatedAtDesc(userId)
			.stream()
			.map(notification -> new NotificationListResponse.NotificationItem(
				notification.getId(),
				notification.getUser().getId(),
				notification.getType(),
				notification.getTitle(),
				notification.getContent(),
				notification.isRead(),
				notification.getCreatedAt()
			))
			.toList();

		return new NotificationListResponse(notifications);
	}

	@Transactional(readOnly = true)
	public NotificationStatusResponse getNotificationStatus(Long userId) {
		long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
		return new NotificationStatusResponse(unreadCount > 0, unreadCount);
	}

	@Transactional
	public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.NOTIFICATION_NOT_FOUND));

		notification.markAsRead();
		return new NotificationReadResponse(notification.getId(), notification.isRead());
	}
}
