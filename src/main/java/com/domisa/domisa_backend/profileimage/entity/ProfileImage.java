package com.domisa.domisa_backend.profileimage.entity;

import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import com.domisa.domisa_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "profile_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "upload_sequence", nullable = false)
	private Long uploadSequence;

	@Column(name = "profile_source_key", length = 1024)
	private String profileSourceKey;

	@Column(name = "profile_detail_blur_key", length = 1024)
	private String profileDetailBlurKey;

	@Column(name = "profile_thumbnail_key", length = 1024)
	private String profileThumbnailKey;

	@Column(name = "profile_thumbnail_blur_key", length = 1024)
	private String profileThumbnailBlurKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "processing_status", length = 20, nullable = false)
	private ProfileImageProcessingStatus processingStatus;

	@Column(name = "retry_count", nullable = false)
	private Integer retryCount;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	private ProfileImage(User user) {
		this.user = user;
		this.uploadSequence = 0L;
		this.processingStatus = ProfileImageProcessingStatus.READY;
		this.retryCount = 0;
		user.setProfileImage(this);
	}

	public static ProfileImage create(User user) {
		return new ProfileImage(user);
	}

	public void prepareUpload(
		long nextSequence,
		String profileSourceKey,
		String profileDetailBlurKey,
		String profileThumbnailKey,
		String profileThumbnailBlurKey
	) {
		this.uploadSequence = nextSequence;
		this.profileSourceKey = profileSourceKey;
		this.profileDetailBlurKey = profileDetailBlurKey;
		this.profileThumbnailKey = profileThumbnailKey;
		this.profileThumbnailBlurKey = profileThumbnailBlurKey;
		this.processingStatus = ProfileImageProcessingStatus.UPLOADING;
		this.retryCount = 0;
		this.lastError = null;
	}

	public void markPending() {
		this.processingStatus = ProfileImageProcessingStatus.PENDING;
		this.lastError = null;
	}

	public void markProcessing() {
		this.processingStatus = ProfileImageProcessingStatus.PROCESSING;
		this.lastError = null;
	}

	public void markReady() {
		this.processingStatus = ProfileImageProcessingStatus.READY;
		this.lastError = null;
	}

	public void markFailed(String lastError) {
		this.processingStatus = ProfileImageProcessingStatus.FAILED;
		this.retryCount = retryCount == null ? 1 : retryCount + 1;
		this.lastError = truncate(lastError);
	}

	public void clear() {
		this.profileSourceKey = null;
		this.profileDetailBlurKey = null;
		this.profileThumbnailKey = null;
		this.profileThumbnailBlurKey = null;
		this.processingStatus = ProfileImageProcessingStatus.READY;
		this.retryCount = 0;
		this.lastError = null;
	}

	public boolean hasSourceKey() {
		return hasText(profileSourceKey);
	}

	public boolean hasAnyKey() {
		return hasSourceKey()
			|| hasText(profileDetailBlurKey)
			|| hasText(profileThumbnailKey)
			|| hasText(profileThumbnailBlurKey);
	}

	public boolean canRetry(int maxRetryCount) {
		return processingStatus == ProfileImageProcessingStatus.PENDING
			|| (processingStatus == ProfileImageProcessingStatus.FAILED
			&& (retryCount == null || retryCount < maxRetryCount));
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String truncate(String value) {
		if (value == null) {
			return null;
		}
		return value.length() <= 1000 ? value : value.substring(0, 1000);
	}
}
