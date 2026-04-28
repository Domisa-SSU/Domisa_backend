package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import java.awt.image.BufferedImage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileImageProcessingService {

	private final ProfileImageRepository profileImageRepository;
	private final ProfileImageStorageService profileImageStorageService;
	private final ProfileImageProcessor profileImageProcessor;
	private final ProfileImageProcessingProperties properties;

	@Transactional
	public void processPendingImages() {
		List<ProfileImage> candidates = profileImageRepository.findByProcessingStatusInOrderByIdAsc(
			List.of(ProfileImageProcessingStatus.PENDING, ProfileImageProcessingStatus.FAILED),
			PageRequest.of(0, properties.getBatchSize())
		);

		for (ProfileImage profileImage : candidates) {
			processProfileImage(profileImage);
		}
	}

	private void processProfileImage(ProfileImage profileImage) {
		if (!profileImage.canRetry(properties.getMaxRetryCount())) {
			return;
		}
		if (!profileImage.hasSourceKey()) {
			return;
		}

		try {
			// The client uploads directly to S3, so the scheduler has to wait until the source object exists.
			if (!profileImageStorageService.exists(profileImage.getProfileSourceKey())) {
				return;
			}

			profileImage.markProcessing();
			byte[] sourceBytes = profileImageStorageService.read(profileImage.getProfileSourceKey());
			BufferedImage sourceImage = profileImageProcessor.read(sourceBytes);
			ProcessedProfileImageSet variants = profileImageProcessor.generateVariants(sourceImage);

			profileImageStorageService.uploadJpeg(profileImage.getProfileThumbnailKey(), variants.thumbnail());
			profileImageStorageService.uploadJpeg(profileImage.getProfileThumbnailBlurKey(), variants.thumbnailBlur());
			profileImageStorageService.uploadJpeg(profileImage.getProfileDetailBlurKey(), variants.detailBlur());
			profileImage.markReady();
		} catch (Exception exception) {
			profileImage.markFailed(exception.getMessage());
		}
	}
}
