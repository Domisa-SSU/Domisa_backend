package com.domisa.domisa_backend.profileimage.scheduler;

import com.domisa.domisa_backend.profileimage.service.ProfileImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageProcessingScheduler {

	private final ProfileImageProcessingService profileImageProcessingService;

	@Scheduled(fixedDelayString = "${app.profile-image.processing.fixed-delay-ms:30000}")
	public void processProfileImages() {
		try {
			// 이전 이미지 처리 작업이 끝난 뒤 다음 배치를 실행한다.
			profileImageProcessingService.processPendingImages();
		} catch (Exception exception) {
			log.error("프로필 이미지 처리 스케줄러 실행 중 예외가 발생했습니다.", exception);
		}
	}
}
