package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.service.S3ObjectStorageService;
import com.domisa.domisa_backend.global.s3.service.S3ProfileImageKeyService;
import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import java.awt.image.BufferedImage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageProcessingService {

	private final ProfileImageRepository profileImageRepository;
	private final S3ObjectStorageService s3ObjectStorageService;
	private final S3ProfileImageKeyService s3ProfileImageKeyService;
	private final ProfileImageProcessor profileImageProcessor;
	private final ProfileImageProcessingProperties properties;

	@Transactional
	public void processPendingImages() {
		// 신규 업로드와 실패 재시도 대상을 배치 단위로 가져온다.
		List<ProfileImage> candidates = profileImageRepository.findByProcessingStatusInOrderByIdAsc(
			List.of(ProfileImageProcessingStatus.PENDING, ProfileImageProcessingStatus.FAILED),
			PageRequest.of(0, properties.getBatchSize())
		);

		if (!candidates.isEmpty()) {
			log.info("프로필 이미지 처리 배치를 시작합니다. candidateCount={}", candidates.size());
		}
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

		long startedAt = System.nanoTime();
		try {
			// 프론트가 presigned PUT으로 직접 업로드하므로 origin 객체가 실제로 생긴 뒤에만 처리한다.
			if (!s3ObjectStorageService.exists(profileImage.getProfileOriginKey())) {
				log.info("프로필 이미지 처리를 건너뜁니다. reason=origin_not_uploaded, profileImageId={}, userId={}",
					profileImage.getId(), profileImage.getUser().getId());
				return;
			}

			profileImage.markProcessing();
			log.info("프로필 이미지 처리를 시작했습니다. profileImageId={}, userId={}, uploadKey={}",
				profileImage.getId(), profileImage.getUser().getId(), profileImage.getProfileOriginKey());
			byte[] originBytes = s3ObjectStorageService.read(profileImage.getProfileOriginKey());
			BufferedImage originImage = profileImageProcessor.read(originBytes);
			ProcessedProfileImageSet variants = profileImageProcessor.generateVariants(originImage);
			String uploadedOriginKey = profileImage.getProfileOriginKey();
			String finalOriginKey = s3ProfileImageKeyService.buildOriginKey(profileImage.getUser());
			String finalOriginBlurKey = s3ProfileImageKeyService.buildOriginBlurKey(profileImage.getUser());
			String finalThumbnailKey = s3ProfileImageKeyService.buildThumbnailKey(profileImage.getUser());
			String finalThumbnailBlurKey = s3ProfileImageKeyService.buildThumbnailBlurKey(profileImage.getUser());

			s3ObjectStorageService.uploadJpeg(finalOriginKey, variants.origin());
			s3ObjectStorageService.uploadJpeg(finalThumbnailKey, variants.thumbnail());
			s3ObjectStorageService.uploadJpeg(finalThumbnailBlurKey, variants.thumbnailBlur());
			s3ObjectStorageService.uploadJpeg(finalOriginBlurKey, variants.originBlur());
			s3ObjectStorageService.deleteAll(List.of(uploadedOriginKey));
			profileImage.markReady(
				finalOriginKey,
				finalOriginBlurKey,
				finalThumbnailKey,
				finalThumbnailBlurKey
			);
			log.info(
				"프로필 이미지 처리를 완료했습니다. profileImageId={}, userId={}, elapsedMs={}, originKey={}",
				profileImage.getId(),
				profileImage.getUser().getId(),
				elapsedMillis(startedAt),
				finalOriginKey
			);
		} catch (Exception exception) {
			profileImage.markFailed(exception.getMessage());
			log.warn(
				"프로필 이미지 처리에 실패했습니다. profileImageId={}, userId={}, elapsedMs={}, reason={}",
				profileImage.getId(),
				profileImage.getUser().getId(),
				elapsedMillis(startedAt),
				exception.getMessage()
			);
		}
	}

	private long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000;
	}
}
