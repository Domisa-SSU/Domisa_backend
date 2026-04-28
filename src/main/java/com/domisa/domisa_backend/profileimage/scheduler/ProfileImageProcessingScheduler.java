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
		// Polling is used because uploads finish outside the application through presigned S3 PUTs.
		profileImageProcessingService.processPendingImages();
	}
}
