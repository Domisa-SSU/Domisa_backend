package com.domisa.domisa_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.dating.dto.DatingMatchCountResponse;
import com.domisa.domisa_backend.dating.service.DatingService;
import com.domisa.domisa_backend.notification.repository.NotificationRepository;
import com.domisa.domisa_backend.sms.client.BizgoClient;
import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import com.domisa.domisa_backend.sms.service.SmsService;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSmsServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private DatingService datingService;

	@Mock
	private BizgoClient bizgoClient;

	@Test
	void sendAllUsersSmsReplacesMatchCountInMessage() {
		SmsService smsService = new SmsService(
			bizgoClient,
			new BizgoProperties("https://mars.ibapi.kr", "test-api-key", "0212345678")
		);
		NotificationSmsService notificationSmsService = new NotificationSmsService(
			notificationRepository,
			userRepository,
			smsService,
			datingService
		);

		when(datingService.getMatchCount()).thenReturn(new DatingMatchCountResponse(324));
		when(userRepository.findAllNotificationPhones()).thenReturn(List.of("010-1234-5678"));

		ArgumentCaptor<SmsRequest> requestCaptor = ArgumentCaptor.forClass(SmsRequest.class);
		notificationSmsService.sendAllUsersSms();

		verify(bizgoClient).send(requestCaptor.capture());
		String text = requestCaptor.getValue().getMessageFlow().getFirst().getSms().getText();
		assertThat(text).contains("현재 324쌍이 매칭됐어요");
		assertThat(text).doesNotContain("{}");
	}

	@Test
	void sendAllUsersSmsWithManualMessageDoesNotApplyByteLimit() {
		SmsService smsService = new SmsService(
			bizgoClient,
			new BizgoProperties("https://mars.ibapi.kr", "test-api-key", "0212345678")
		);
		NotificationSmsService notificationSmsService = new NotificationSmsService(
			notificationRepository,
			userRepository,
			smsService,
			datingService
		);

		String longMessage = "a".repeat(200);
		when(userRepository.findAllNotificationPhones()).thenReturn(List.of("010-1234-5678"));

		notificationSmsService.sendAllUsersSms(longMessage);

		verify(bizgoClient).send(org.mockito.ArgumentMatchers.any(SmsRequest.class));
		verify(userRepository).findAllNotificationPhones();
		verify(datingService, org.mockito.Mockito.never()).getMatchCount();
	}
}
