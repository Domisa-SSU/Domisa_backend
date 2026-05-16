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
	private static final String ALL_USERS_MESSAGE = """
		(광고) 솔로개발자가 문자종료를
		깜빡했어요. 이벤트 10시간 연장!
		진짜 마지막.
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
		log.info("읽지 않은 알림 SMS 대상 알림을 조회했습니다. notificationCount={}", notifications.size());
		if (notifications.isEmpty()) {
			log.info("문자로 보낼 읽지 않은 알림이 없습니다.");
			return;
		}

		Map<Long, Set<NotificationType>> typesByUserId = notifications.stream()
			.collect(Collectors.groupingBy(
				Notification::getUserId,
				Collectors.mapping(Notification::getType, Collectors.toCollection(() -> EnumSet.noneOf(NotificationType.class)))
		));
		log.info("읽지 않은 알림 SMS 대상 유저를 집계했습니다. userCount={}, userIds={}", typesByUserId.size(), typesByUserId.keySet());

		Map<Long, User> usersById = userRepository.findActiveAllByIdIn(typesByUserId.keySet()).stream()
			.collect(Collectors.toMap(User::getId, user -> user));
		log.info("읽지 않은 알림 SMS 대상 유저 정보를 조회했습니다. foundUserCount={}", usersById.size());

		List<String> phones = typesByUserId.entrySet().stream()
			.filter(entry -> isSmsTarget(
				entry.getKey(),
				entry.getValue(),
				usersById.get(entry.getKey())
			))
			.map(entry -> usersById.get(entry.getKey()).getNotificationPhone())
			.toList();
		if (phones.isEmpty()) {
			log.info("읽지 않은 알림 SMS 최종 발송 대상 전화번호가 없습니다.");
			return;
		}

		try {
			log.info("읽지 않은 알림 SMS 동보 발송을 요청합니다. count={}, phones={}", phones.size(), maskPhones(phones));
			smsService.sendAll(phones, UNREAD_NOTIFICATION_MESSAGE);
			log.info("읽지 않은 알림 SMS를 동보 발송했습니다. count={}", phones.size());
		} catch (RuntimeException exception) {
			log.warn("읽지 않은 알림 SMS 동보 발송에 실패했습니다. count={}, reason={}", phones.size(), exception.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public void sendAllUsersSms() {
		sendAllUsersSms(ALL_USERS_MESSAGE);
	}

	@Transactional(readOnly = true)
	public void sendAllUsersSms(String message) {
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("message는 필수입니다.");
		}
		List<String> phones = userRepository.findAllNotificationPhones();
		log.info("전체 유저 SMS 대상 전화번호를 조회했습니다. count={}", phones.size());
		if (phones.isEmpty()) {
			log.info("전체 유저 SMS 발송 대상 전화번호가 없습니다.");
			return;
		}

		try {
			log.info("전체 유저 SMS 동보 발송을 요청합니다. count={}, phones={}", phones.size(), maskPhones(phones));
			smsService.sendAll(phones, message);
			log.info("전체 유저 SMS를 동보 발송했습니다. count={}", phones.size());
		} catch (RuntimeException exception) {
			log.warn("전체 유저 SMS 동보 발송에 실패했습니다. count={}, reason={}", phones.size(), exception.getMessage());
		}
	}

	private String buildMessage(Set<NotificationType> types) {
		if (types.contains(NotificationType.LIKE) || types.contains(NotificationType.MATCH)) {
			return UNREAD_NOTIFICATION_MESSAGE;
		}
		return null;
	}

	private boolean isSmsTarget(Long userId, Set<NotificationType> types, User user) {
		String message = buildMessage(types);
		if (message == null) {
			log.info("읽지 않은 알림 SMS 대상에서 제외했습니다. userId={}, types={}, reason=no_message", userId, types);
			return false;
		}
		if (user == null) {
			log.info("읽지 않은 알림 SMS 대상에서 제외했습니다. userId={}, types={}, reason=user_not_found", userId, types);
			return false;
		}
		String phone = user.getNotificationPhone();
		if (phone == null || phone.isBlank()) {
			log.info("읽지 않은 알림 SMS 대상에서 제외했습니다. userId={}, publicId={}, types={}, reason=no_phone",
				user.getId(), user.getPublicId(), types);
			return false;
		}
		log.info("읽지 않은 알림 SMS 발송 대상입니다. userId={}, publicId={}, types={}, phone={}",
			user.getId(), user.getPublicId(), types, maskPhone(phone));
		return true;
	}

	private List<String> maskPhones(List<String> phones) {
		return phones.stream()
			.map(this::maskPhone)
			.toList();
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return "";
		}
		String normalizedPhone = phone.replaceAll("[^0-9]", "");
		if (normalizedPhone.length() <= 4) {
			return "****";
		}
		return "***-****-" + normalizedPhone.substring(normalizedPhone.length() - 4);
	}
}
