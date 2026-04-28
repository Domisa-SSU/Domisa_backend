package com.domisa.domisa_backend.profileimage.scheduler;

import com.domisa.domisa_backend.profileimage.service.ProfileImageProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageProcessingScheduler {

	private final ProfileImageProcessingService profileImageProcessingService;

	@Scheduled(fixedDelayString = "${app.profile-image.processing.fixed-delay-ms:30000}")
	public void processProfileImages() {
		// presigned PUT 업로드 완료를 앱이 직접 알 수 없어서 주기적으로 상태를 확인한다.
		profileImageProcessingService.processPendingImages();
	}
}
