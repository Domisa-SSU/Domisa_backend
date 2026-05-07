package com.domisa.domisa_backend.dating.scheduler;

import com.domisa.domisa_backend.dating.service.DatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatingProfileRefreshScheduler {

	private final DatingService datingService;

	@Scheduled(cron = "0 * * * * *")
	public void refreshReadyNowShows() {
		datingService.refreshReadyNowShows();
	}
}
