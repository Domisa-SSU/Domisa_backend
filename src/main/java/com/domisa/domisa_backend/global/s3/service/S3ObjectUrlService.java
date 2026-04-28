package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
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
		if (profileImage == null || !profileImage.hasSourceKey()) {
			return null;
		}
		return buildObjectUrl(profileImage.getProfileSourceKey());
	}

	public String getObjectUrl(String objectKey) {
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
}
