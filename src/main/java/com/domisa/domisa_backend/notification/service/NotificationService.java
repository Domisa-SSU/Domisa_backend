package com.domisa.domisa_backend.notification.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.notification.dto.NotificationListResponse;
import com.domisa.domisa_backend.notification.dto.NotificationReadResponse;
import com.domisa.domisa_backend.notification.dto.NotificationStatusResponse;
import com.domisa.domisa_backend.notification.entity.Notification;
import com.domisa.domisa_backend.notification.repository.NotificationRepository;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Transactional
	public Notification createNotification(NotificationType type, Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		return createNotification(type, user);
	}

	@Transactional
	public Notification createNotification(NotificationType type, User user) {
		Notification.NotificationTemplate template = createTemplate(type);
		Notification notification = Notification.create(user, template);
		return notificationRepository.save(notification);
	}

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

	private Notification.NotificationTemplate createTemplate(NotificationType type) {
		return switch (type) {
			case LIKE -> new Notification.NotificationTemplate(
				NotificationType.LIKE,
				"새로운 호감이 도착했어요",
				"새로운 호감을 확인해보세요."
			);
			case INTRODUCTION -> new Notification.NotificationTemplate(
				NotificationType.INTRODUCTION,
				"소개서가 도착했어요",
				"새로운 소개서를 확인해보세요."
			);
			case SHUFFLE -> new Notification.NotificationTemplate(
				NotificationType.SHUFFLE,
				"새로운 추천이 도착했어요",
				"새로운 추천 상대를 확인해보세요."
			);
		};
	}
}
