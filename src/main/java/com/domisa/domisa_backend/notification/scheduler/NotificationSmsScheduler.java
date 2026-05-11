package com.domisa.domisa_backend.notification.scheduler;

import com.domisa.domisa_backend.notification.service.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSmsScheduler {

	private final NotificationSmsService notificationSmsService;

	@Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSmsAtTenPm() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 30 22 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSmsAtTenThirtyPm() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSmsAtElevenPm() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 45 23 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSmsT4() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 57 23 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSmsT5() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 57 23 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSmsT6() {
		sendUnreadNotificationSms();
	}


	public void sendUnreadNotificationSms() {
		try {
			notificationSmsService.sendUnreadNotificationSms();
		} catch (Exception exception) {
			log.error("읽지 않은 알림 SMS 스케줄러 실행 중 예외가 발생했습니다.", exception);
		}
	}
}
