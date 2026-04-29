package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.service.S3ObjectStorageService;
import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
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
	private final S3ObjectStorageService s3ObjectStorageService;
	private final ProfileImageProcessor profileImageProcessor;
	private final ProfileImageProcessingProperties properties;

	@Transactional
	public void processPendingImages() {
		// 신규 업로드와 실패 재시도 대상을 배치 단위로 가져온다.
		List<ProfileImage> candidates = profileImageRepository.findByProcessingStatusInOrderByIdAsc(
			List.of(ProfileImageProcessingStatus.PENDING, ProfileImageProcessingStatus.FAILED),
			PageRequest.of(0, properties.getBatchSize())
		);

		for (ProfileImage profileImage : candidates) {
			processProfileImage(profileImage);
		}
	}

	private void processProfileImage(ProfileImage profileImage) {
		// 재시도 한도를 넘기거나 origin이 없으면 이번 턴에는 건너뛴다.
		if (!profileImage.canRetry(properties.getMaxRetryCount())) {
			return;
		}
		if (!profileImage.hasOriginKey()) {
			return;
		}

		try {
			// 프론트가 presigned PUT으로 직접 업로드하므로 origin 객체가 실제로 생긴 뒤에만 처리한다.
			if (!s3ObjectStorageService.exists(profileImage.getProfileOriginKey())) {
				return;
			}

			profileImage.markProcessing();
			byte[] originBytes = s3ObjectStorageService.read(profileImage.getProfileOriginKey());
			BufferedImage originImage = profileImageProcessor.read(originBytes);
			ProcessedProfileImageSet variants = profileImageProcessor.generateVariants(originImage);

			s3ObjectStorageService.uploadJpeg(profileImage.getProfileThumbnailKey(), variants.thumbnail());
			s3ObjectStorageService.uploadJpeg(profileImage.getProfileThumbnailBlurKey(), variants.thumbnailBlur());
			s3ObjectStorageService.uploadJpeg(profileImage.getProfileOriginBlurKey(), variants.originBlur());
			profileImage.markReady();
		} catch (Exception exception) {
			profileImage.markFailed(exception.getMessage());
		}
	}
}
