package com.domisa.domisa_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.notification.entity.Notification;
import com.domisa.domisa_backend.notification.repository.NotificationRepository;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.sms.service.SmsService;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSmsServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SmsService smsService;

	@InjectMocks
	private NotificationSmsService notificationSmsService;

	@Test
	void sendUnreadNotificationSmsUsesBroadcastSend() {
		Notification like = Notification.create(1L, 2L, NotificationType.LIKE);
		Notification match = Notification.create(2L, 1L, NotificationType.MATCH);
		User user1 = User.create(100L);
		user1.setId(1L);
		user1.setNotificationPhone("010-1111-1111");
		User user2 = User.create(200L);
		user2.setId(2L);
		user2.setNotificationPhone("010-2222-2222");
		when(notificationRepository.findUnreadSmsTargetNotifications(anyList())).thenReturn(List.of(like, match));
		when(userRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(user1, user2));
		ArgumentCaptor<Collection<String>> phonesCaptor = ArgumentCaptor.forClass(Collection.class);
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

		notificationSmsService.sendUnreadNotificationSms();

		verify(smsService).sendAll(phonesCaptor.capture(), messageCaptor.capture());
		assertThat(phonesCaptor.getValue()).containsExactlyInAnyOrder("010-1111-1111", "010-2222-2222");
		assertThat(messageCaptor.getValue()).contains("도미사럽에서 바로 확인해보세요");
	}
}
