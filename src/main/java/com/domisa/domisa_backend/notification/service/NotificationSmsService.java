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

	private static final String UNREAD_NOTIFICATION_MESSAGE = """
		누군가 나에게 호감을 보냈어요 ♥
		도미사럽에서 바로 확인해보세요
		https://domisalove.me/
		""".strip();

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

		List<String> phones = typesByUserId.entrySet().stream()
			.filter(entry -> buildMessage(entry.getValue()) != null)
			.map(entry -> usersById.get(entry.getKey()))
			.filter(user -> user != null && user.getNotificationPhone() != null && !user.getNotificationPhone().isBlank())
			.map(User::getNotificationPhone)
			.toList();
		if (phones.isEmpty()) {
			return;
		}

		try {
			smsService.sendAll(phones, UNREAD_NOTIFICATION_MESSAGE);
			log.info("읽지 않은 알림 SMS를 동보 발송했습니다. count={}", phones.size());
		} catch (RuntimeException exception) {
			log.warn("읽지 않은 알림 SMS 동보 발송에 실패했습니다. count={}, reason={}", phones.size(), exception.getMessage());
		}
	}

	private String buildMessage(Set<NotificationType> types) {
		if (types.contains(NotificationType.LIKE) || types.contains(NotificationType.MATCH)) {
			return UNREAD_NOTIFICATION_MESSAGE;
		}
		return null;
	}
}
