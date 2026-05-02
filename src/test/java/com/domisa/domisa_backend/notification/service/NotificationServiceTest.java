package com.domisa.domisa_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.notification.dto.NotificationActiveResponse;
import com.domisa.domisa_backend.notification.dto.NotificationListResponse;
import com.domisa.domisa_backend.notification.dto.NotificationStatusResponse;
import com.domisa.domisa_backend.notification.entity.Notification;
import com.domisa.domisa_backend.notification.repository.NotificationRepository;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private NotificationService notificationService;

	@Test
	void getNotificationsReturnsNotificationPayloadForUser() {
		Notification notification = Notification.create(1L, 2L, NotificationType.LIKE);
		ReflectionTestUtils.setField(notification, "id", 10L);
		ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 4, 10, 12, 30));

		User owner = User.create(100L);
		owner.setId(1L);
		User targetUser = User.create(200L);
		targetUser.setId(2L);
		targetUser.setAnimalProfile(AnimalProfile.OTTER);
		targetUser.setNickname("배고파요");

		when(notificationRepository.findAllByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(notification));
		when(userRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(owner, targetUser));

		NotificationListResponse response = notificationService.getNotifications(1L);

		assertThat(response.notifications()).hasSize(1);
		NotificationListResponse.NotificationItem item = response.notifications().getFirst();
		assertThat(item.notificationId()).isEqualTo(10L);
		assertThat(item.userId()).isEqualTo(1L);
		assertThat(item.type()).isEqualTo(NotificationType.LIKE);
		assertThat(item.targetUserId()).isEqualTo(2L);
		assertThat(item.animalProfile()).isEqualTo(AnimalProfile.OTTER);
		assertThat(item.personNickname()).isEqualTo("배고파요");
		assertThat(item.isRead()).isFalse();
		assertThat(item.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 10, 12, 30));
	}

	@Test
	void getActiveNotificationsAggregatesByType() {
		Notification signup = Notification.create(1L, NotificationType.SIGNUP);
		Notification referral1 = Notification.create(1L, NotificationType.REFERRAL);
		Notification referral2 = Notification.create(1L, NotificationType.REFERRAL);
		Notification match = Notification.create(1L, 2L, NotificationType.MATCH);
		when(notificationRepository.findAllByUserIdAndIsCanceledFalseOrderByCreatedAtAsc(1L)).thenReturn(
			List.of(signup, referral1, referral2, match)
		);

		NotificationActiveResponse response = notificationService.getActiveNotifications(1L);

		assertThat(response.signup()).isTrue();
		assertThat(response.referralCount()).isEqualTo(2);
		assertThat(response.like()).isFalse();
		assertThat(response.match()).isTrue();
		assertThat(signup.isRead()).isTrue();
		assertThat(referral1.isRead()).isTrue();
		assertThat(referral2.isRead()).isTrue();
		assertThat(match.isRead()).isFalse();
		assertThat(signup.isCanceled()).isTrue();
		assertThat(referral1.isCanceled()).isTrue();
		assertThat(referral2.isCanceled()).isTrue();
		assertThat(match.isCanceled()).isTrue();
	}

	@Test
	void getNotificationStatusReturnsUnreadSummary() {
		when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

		NotificationStatusResponse response = notificationService.getNotificationStatus(1L);

		assertThat(response.hasUnread()).isTrue();
		assertThat(response.unreadCount()).isEqualTo(3L);
	}

	@Test
	void markAsReadMarksOwnedNotification() {
		Notification notification = Notification.create(1L, 2L, NotificationType.LIKE);
		when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

		notificationService.markAsRead(1L, 10L);

		assertThat(notification.isRead()).isTrue();
		assertThat(notification.isCanceled()).isFalse();
	}
}
