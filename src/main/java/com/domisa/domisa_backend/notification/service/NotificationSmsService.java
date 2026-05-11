package com.domisa.domisa_backend.notification.service;

import com.domisa.domisa_backend.notification.entity.Notification;
import com.domisa.domisa_backend.notification.repository.NotificationRepository;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.sms.service.SmsService;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSmsService {

	private static final String LIKE_MESSAGE = "호감을 보냈습니다";
	private static final String MATCH_MESSAGE = "쌍방 매칭이 이뤄졌어요";

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SmsService smsService;

	@Transactional(readOnly = true)
	public void sendUnreadNotificationSms() {
		List<Notification> notifications = notificationRepository.findUnreadSmsTargetNotifications(List.of(
			NotificationType.LIKE,
			NotificationType.MATCH
		));
		if (notifications.isEmpty()) {
			log.info("문자로 보낼 읽지 않은 알림이 없습니다.");
			return;
		}

		Map<Long, Set<NotificationType>> typesByUserId = notifications.stream()
			.collect(Collectors.groupingBy(
				Notification::getUserId,
				Collectors.mapping(Notification::getType, Collectors.toCollection(() -> EnumSet.noneOf(NotificationType.class)))
			));

		Map<Long, User> usersById = userRepository.findAllByIdIn(typesByUserId.keySet()).stream()
			.collect(Collectors.toMap(User::getId, user -> user));

		typesByUserId.forEach((userId, types) -> sendToUser(usersById.get(userId), types));
	}

	private void sendToUser(User user, Set<NotificationType> types) {
		if (user == null || user.getNotificationPhone() == null || user.getNotificationPhone().isBlank()) {
			return;
		}

		String message = buildMessage(types);
		if (message == null) {
			return;
		}

		try {
			smsService.send(user.getNotificationPhone(), message);
			log.info("읽지 않은 알림 SMS를 발송했습니다. userId={}, types={}", user.getId(), types);
		} catch (RuntimeException exception) {
			log.warn("읽지 않은 알림 SMS 발송에 실패했습니다. userId={}, reason={}", user.getId(), exception.getMessage());
		}
	}

	private String buildMessage(Set<NotificationType> types) {
		boolean hasLike = types.contains(NotificationType.LIKE);
		boolean hasMatch = types.contains(NotificationType.MATCH);
		if (hasLike && hasMatch) {
			return LIKE_MESSAGE + "\n" + MATCH_MESSAGE;
		}
		if (hasLike) {
			return LIKE_MESSAGE;
		}
		if (hasMatch) {
			return MATCH_MESSAGE;
		}
		return null;
	}
}
