package com.domisa.domisa_backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationLegacyCleanupService implements ApplicationRunner {

	private static final String LEGACY_COOKIE_PAYMENT_TYPE = "COOKIE_PAYMENT";

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void run(ApplicationArguments args) {
		int deletedCount = jdbcTemplate.update(
			"delete from notifications where type = ?",
			LEGACY_COOKIE_PAYMENT_TYPE
		);
		if (deletedCount > 0) {
			log.info("사용하지 않는 쿠키 결제 알림을 삭제했습니다. count={}", deletedCount);
		}
	}
}
