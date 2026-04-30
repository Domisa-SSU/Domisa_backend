package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Service
@RequiredArgsConstructor
public class S3ObjectUrlService {

	private final S3Client s3Client;
	private final S3Properties s3Properties;

	public String getProfileImageUrl(ProfileImage profileImage) {
		// 일반 프로필 조회는 origin 이미지를 기준으로 응답한다.
		if (profileImage == null || !profileImage.hasOriginKey()) {
			return null;
		}
		return buildStoredObjectUrl(profileImage.getProfileOriginKey());
	}

	public String getThumbnailUrl(ProfileImage profileImage) {
		// 썸네일은 READY 상태에서만 사용하고, 그 전에는 origin 이미지로만 폴백한다.
		if (profileImage == null) {
			return null;
		}
		if (isReady(profileImage) && hasText(profileImage.getProfileThumbnailKey())) {
			return buildStoredObjectUrl(profileImage.getProfileThumbnailKey());
		}
		if (profileImage.hasOriginKey()) {
			return buildStoredObjectUrl(profileImage.getProfileOriginKey());
		}
		return null;
	}

	public String getThumbnailBlurUrl(ProfileImage profileImage) {
		// 블러 썸네일도 READY 상태에서만 사용하고, 없으면 일반 썸네일까지만 폴백한다.
		if (profileImage == null) {
			return null;
		}
		if (isReady(profileImage) && hasText(profileImage.getProfileThumbnailBlurKey())) {
			return buildStoredObjectUrl(profileImage.getProfileThumbnailBlurKey());
		}
		if (isReady(profileImage) && hasText(profileImage.getProfileThumbnailKey())) {
			return buildStoredObjectUrl(profileImage.getProfileThumbnailKey());
		}
		return null;
	}

	public String getOriginBlurUrl(ProfileImage profileImage) {
		// 블러 프로필 상세는 READY 상태의 origin blur만 사용한다.
		if (profileImage == null) {
			return null;
		}
		if (isReady(profileImage) && hasText(profileImage.getProfileOriginBlurKey())) {
			return buildStoredObjectUrl(profileImage.getProfileOriginBlurKey());
		}
		return null;
	}

	public String getObjectUrl(String objectKey) {
		// 임의 key 조회는 존재 여부를 확인한 뒤 URL을 만든다.
		String normalizedObjectKey = normalizeObjectKey(objectKey);

		try {
			s3Client.headObject(HeadObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(normalizedObjectKey)
				.build());
			return buildObjectUrl(normalizedObjectKey);
		} catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
			if (exception.statusCode() == 404) {
				throw new S3Exception(S3ErrorCode.OBJECT_NOT_FOUND);
			}
			throw new S3Exception(S3ErrorCode.OBJECT_URL_RESOLUTION_FAILED, exception);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.OBJECT_URL_RESOLUTION_FAILED, exception);
		}
	}

	public String buildStoredObjectUrl(String objectKey) {
		// 이미 저장된 내부 key는 바로 URL로 변환한다.
		return buildObjectUrl(normalizeObjectKey(objectKey));
	}

	private String normalizeObjectKey(String objectKey) {
		String normalized = objectKey == null ? "" : objectKey.strip().replace('\\', '/');
		if (normalized.isBlank() || normalized.contains("..") || normalized.startsWith("/")) {
			throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
		}
		return normalized.replaceAll("/+", "/");
	}

	public String buildObjectUrl(String objectKey) {
		return "https://" + s3Properties.bucket()
			+ ".s3." + s3Properties.region()
			+ ".amazonaws.com/"
			+ UriUtils.encodePath(objectKey, StandardCharsets.UTF_8);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private boolean isReady(ProfileImage profileImage) {
		return profileImage.getProcessingStatus() == ProfileImageProcessingStatus.READY;
	}
}
