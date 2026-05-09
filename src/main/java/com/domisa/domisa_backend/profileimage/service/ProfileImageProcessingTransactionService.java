package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileImageProcessingTransactionService {

	private static final int MAX_LAST_ERROR_LENGTH = 1000;
	private static final int MAX_RETRY_COUNT = 1;

	private final ProfileImageRepository profileImageRepository;

	@Transactional(readOnly = true)
	public List<Long> findPendingImageIds(int batchSize) {
		return profileImageRepository.findRetryableIdsOrderByIdAsc(
			ProfileImageProcessingStatus.PENDING,
			ProfileImageProcessingStatus.FAILED,
			MAX_RETRY_COUNT,
			PageRequest.of(0, batchSize)
		);
	}

	@Transactional
	public boolean markAsProcessing(Long profileImageId) {
		return profileImageRepository.markAsProcessing(
			profileImageId,
			ProfileImageProcessingStatus.PROCESSING,
			ProfileImageProcessingStatus.PENDING,
			ProfileImageProcessingStatus.FAILED,
			MAX_RETRY_COUNT
		) == 1;
	}

	@Transactional(readOnly = true)
	public Optional<ProfileImage> findProcessingImage(Long profileImageId) {
		return profileImageRepository.findByIdWithUser(profileImageId)
			.filter(profileImage -> profileImage.getProcessingStatus() == ProfileImageProcessingStatus.PROCESSING);
	}

	@Transactional
	public boolean markAsCompleted(
		Long profileImageId,
		Long uploadSequence,
		String profileOriginKey,
		String profileOriginBlurKey,
		String profileThumbnailKey,
		String profileThumbnailBlurKey
	) {
		return profileImageRepository.markAsCompleted(
			profileImageId,
			uploadSequence,
			profileOriginKey,
			profileOriginBlurKey,
			profileThumbnailKey,
			profileThumbnailBlurKey,
			ProfileImageProcessingStatus.COMPLETED,
			ProfileImageProcessingStatus.PROCESSING
		) == 1;
	}

	@Transactional
	public boolean markAsFailed(Long profileImageId, Long uploadSequence, String lastError) {
		return profileImageRepository.markAsFailed(
			profileImageId,
			uploadSequence,
			truncate(lastError),
			ProfileImageProcessingStatus.FAILED,
			ProfileImageProcessingStatus.PROCESSING
		) == 1;
	}

	private String truncate(String value) {
		if (value == null) {
			return null;
		}
		return value.length() <= MAX_LAST_ERROR_LENGTH ? value : value.substring(0, MAX_LAST_ERROR_LENGTH);
	}
}
