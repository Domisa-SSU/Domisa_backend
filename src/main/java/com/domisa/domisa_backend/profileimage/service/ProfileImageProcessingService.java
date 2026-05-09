package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.service.S3ObjectStorageService;
import com.domisa.domisa_backend.global.s3.service.S3ProfileImageKeyService;
import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import java.awt.image.BufferedImage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageProcessingService {

	private final S3ObjectStorageService s3ObjectStorageService;
	private final S3ProfileImageKeyService s3ProfileImageKeyService;
	private final ProfileImageProcessor profileImageProcessor;
	private final ProfileImageProcessingTransactionService transactionService;
	private final ProfileImageProcessingProperties properties;

	public void processPendingImages() {
		// 신규 업로드 대상을 배치 단위로 가져온 뒤, 각 row를 짧은 트랜잭션으로 선점한다.
		List<Long> candidateIds = transactionService.findPendingImageIds(properties.getBatchSize());

		if (!candidateIds.isEmpty()) {
			log.info("프로필 이미지 처리 배치를 시작합니다. candidateCount={}", candidateIds.size());
		}
		for (Long profileImageId : candidateIds) {
			processProfileImage(profileImageId);
		}
	}

	private void processProfileImage(Long profileImageId) {
		if (!transactionService.markAsProcessing(profileImageId)) {
			log.info("이미 다른 작업자가 프로필 이미지를 처리 중이라 건너뜁니다. profileImageId={}", profileImageId);
			return;
		}

		ProfileImage profileImage = transactionService.findProcessingImage(profileImageId)
			.orElse(null);
		if (profileImage == null) {
			log.info("선점한 프로필 이미지를 다시 조회하지 못해 처리를 건너뜁니다. profileImageId={}", profileImageId);
			return;
		}

		Long uploadSequence = profileImage.getUploadSequence();
		if (!profileImage.hasOriginKey()) {
			markFailed(profileImage, uploadSequence, "원본 이미지 키가 없습니다.", System.nanoTime());
			return;
		}

		long startedAt = System.nanoTime();
		try {
			if (!s3ObjectStorageService.exists(profileImage.getProfileOriginKey())) {
				markFailed(profileImage, uploadSequence, "원본 이미지가 S3에 없습니다.", startedAt);
				log.info("프로필 이미지 처리를 실패 처리했습니다. reason=origin_not_uploaded, profileImageId={}, userId={}",
					profileImage.getId(), profileImage.getUser().getId());
				return;
			}

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
			boolean completed = transactionService.markAsCompleted(
				profileImage.getId(),
				uploadSequence,
				finalOriginKey,
				finalOriginBlurKey,
				finalThumbnailKey,
				finalThumbnailBlurKey
			);
			if (!completed) {
				log.info(
					"프로필 이미지 완료 상태 반영을 건너뜁니다. reason=status_or_upload_sequence_changed, profileImageId={}, userId={}",
					profileImage.getId(),
					profileImage.getUser().getId()
				);
				return;
			}
			log.info(
				"프로필 이미지 처리를 완료했습니다. profileImageId={}, userId={}, elapsedMs={}, originKey={}",
				profileImage.getId(),
				profileImage.getUser().getId(),
				elapsedMillis(startedAt),
				finalOriginKey
			);
		} catch (Exception exception) {
			markFailed(profileImage, uploadSequence, exception.getMessage(), startedAt);
		}
	}

	private void markFailed(ProfileImage profileImage, Long uploadSequence, String reason, long startedAt) {
		boolean failed = transactionService.markAsFailed(profileImage.getId(), uploadSequence, reason);
		log.warn(
			"프로필 이미지 처리에 실패했습니다. profileImageId={}, userId={}, elapsedMs={}, statusUpdated={}, reason={}",
			profileImage.getId(),
			profileImage.getUser().getId(),
			elapsedMillis(startedAt),
			failed,
			reason
		);
	}

	private long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000;
	}
}
