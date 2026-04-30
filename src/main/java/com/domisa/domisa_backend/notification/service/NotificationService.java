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
import com.domisa.domisa_backend.user.type.AnimalProfile;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;


	@Transactional
	public Notification createNotification(NotificationType type, Long userId, Long targetUserId) {
		validateUserExists(userId);

		Notification notification;
		if (requiresTargetUser(type)) {
			if (targetUserId == null) {
				throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
			}
			validateUserExists(targetUserId);
			notification = Notification.create(userId, targetUserId, type);
		} else {
			notification = Notification.create(userId, type);
		}

		return notificationRepository.save(notification);
	}

	@Transactional(readOnly = true)
	public NotificationListResponse getNotifications(Long userId) {
		List<Notification> storedNotifications = notificationRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
		Map<Long, User> usersById = getUsersById(storedNotifications);
		List<NotificationListResponse.NotificationItem> notifications = storedNotifications
			.stream()
			.map(notification -> new NotificationListResponse.NotificationItem(
				notification.getId(),
				getPublicId(usersById.get(notification.getUserId())),
				notification.getType(),
				getPublicId(usersById.get(notification.getTargetUserId())),
				getAnimalProfile(usersById.get(notification.getTargetUserId())),
				getPersonNickname(usersById.get(notification.getTargetUserId())),
				notification.isRead(),
				notification.getCreatedAt()
			))
			.toList();

		return new NotificationListResponse(notifications);
	}

	@Transactional(readOnly = true)
	public NotificationStatusResponse getNotificationStatus(Long userId) {
		long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
		return new NotificationStatusResponse(unreadCount);
	}

	@Transactional
	public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.NOTIFICATION_NOT_FOUND));

		notification.markAsRead();
		return new NotificationReadResponse(notification.getId(), notification.isRead());
	}


	private void validateUserExists(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}
	}

	private boolean requiresTargetUser(NotificationType type) {
		return type == NotificationType.LIKE || type == NotificationType.MATCH;
	}

	private Map<Long, User> getUsersById(List<Notification> notifications) {
		Set<Long> userIds = new HashSet<>();
		for (Notification notification : notifications) {
			if (notification.getUserId() != null) {
				userIds.add(notification.getUserId());
			}
			if (notification.getTargetUserId() != null) {
				userIds.add(notification.getTargetUserId());
			}
		}

		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}

		return userRepository.findAllByIdIn(userIds).stream()
			.collect(Collectors.toMap(User::getId, user -> user));
	}

	private String getPublicId(User user) {
		return user == null ? null : user.getPublicId();
	}

	private AnimalProfile getAnimalProfile(User user) {
		return user == null ? null : user.getAnimalProfile();
	}

	private String getPersonNickname(User user) {
		return user == null ? null : user.getNickname();
	}
}
