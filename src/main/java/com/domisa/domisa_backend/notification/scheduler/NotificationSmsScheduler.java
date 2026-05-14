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

	@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms1() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms2() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms3() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 13 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms4() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 15 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms5() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 17 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms6() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 19 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms7() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms8() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
	public void sendUnreadNotificationSms9() {
		sendUnreadNotificationSms();
	}

	@Scheduled(cron = "0 30 18 * * *", zone = "Asia/Seoul")
	public void sendAllUsersSms() {
		try {
			notificationSmsService.sendAllUsersSms();
		} catch (Exception exception) {
			log.error("전체 유저 SMS 스케줄러 실행 중 예외가 발생했습니다.", exception);
		}
	}

	public void sendUnreadNotificationSms() {
		try {
			notificationSmsService.sendUnreadNotificationSms();
		} catch (Exception exception) {
			log.error("읽지 않은 알림 SMS 스케줄러 실행 중 예외가 발생했습니다.", exception);
		}
	}
}
