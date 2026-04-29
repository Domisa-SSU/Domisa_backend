package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.global.s3.dto.CompleteProfileImageUploadRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlResponse;
import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

	private static final int MAX_EXTENSION_LENGTH = 20;

	private final UserRepository userRepository;
	private final ProfileImageRepository profileImageRepository;
	private final S3ProfileImageKeyService s3ProfileImageKeyService;
	private final S3ObjectStorageService s3ObjectStorageService;
	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;

	@Transactional
	public GeneratePresignedUploadUrlResponse issueProfileImageUploadUrl(User authUser, GeneratePresignedUploadUrlRequest request) {
		// 업로드 전에는 source key와 파생본 key만 먼저 확정해 둔다.
		User user = getRequiredUser(authUser);
		return createPresignedUploadUrl(user, request);
	}

	@Transactional
	public void completeProfileImageUpload(User authUser, CompleteProfileImageUploadRequest request) {
		// 실제 업로드된 origin 객체를 확인한 뒤 처리 대기 상태로 넘긴다.
		User user = getRequiredUser(authUser);
		ProfileImage profileImage = user.getProfileImage();

		if (profileImage == null || !profileImage.hasOriginKey()) {
			throw new S3Exception(S3ErrorCode.PROFILE_IMAGE_NOT_FOUND);
		}

		String normalizedUploadKey = request.uploadKey().strip();
		if (!profileImage.getProfileOriginKey().equals(normalizedUploadKey)) {
			throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
		}
		if (!s3ObjectStorageService.exists(normalizedUploadKey)) {
			throw new S3Exception(S3ErrorCode.OBJECT_NOT_FOUND);
		}

		profileImage.markPending();
	}

	@Transactional
	public void deleteProfileImage(User authUser) {
		User user = getRequiredUser(authUser);
		deleteProfileImageByUser(user);
	}

	@Transactional
	public void deleteProfileImageByUser(User user) {
		ProfileImage profileImage = user.getProfileImage();
		if (profileImage == null || !profileImage.hasAnyKey()) {
			throw new S3Exception(S3ErrorCode.PROFILE_IMAGE_NOT_FOUND);
		}

		s3ObjectStorageService.deleteAll(List.of(
			profileImage.getProfileOriginKey(),
			profileImage.getProfileThumbnailKey(),
			profileImage.getProfileThumbnailBlurKey(),
			profileImage.getProfileOriginBlurKey()
		));
		profileImageRepository.delete(profileImage);
		user.setProfileImage(null);
	}

	private GeneratePresignedUploadUrlResponse createPresignedUploadUrl(User user, GeneratePresignedUploadUrlRequest request) {
		// origin은 temp 경로로 받고, 썸네일/블러 경로는 미리 저장해 둔다.
		MediaType mediaType = normalizeContentType(request.contentType());
		ProfileImage profileImage = getOrCreateProfileImage(user);
		long uploadSequence = getNextUploadSequence(profileImage);
		String contentType = mediaType.toString();
		String extension = extractExtension(mediaType);
		String originKey = s3ProfileImageKeyService.buildOriginKey(user, uploadSequence, extension);
		String thumbnailKey = s3ProfileImageKeyService.buildThumbnailKey(user, uploadSequence);
		String thumbnailBlurKey = s3ProfileImageKeyService.buildThumbnailBlurKey(user, uploadSequence);
		String originBlurKey = s3ProfileImageKeyService.buildOriginBlurKey(user, uploadSequence);

		profileImage.prepareUpload(uploadSequence, originKey, originBlurKey, thumbnailKey, thumbnailBlurKey);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Properties.bucket())
			.key(originKey)
			.contentType(contentType)
			.contentLength(request.fileSize())
			.build();

		PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(s3Properties.presignedUrlExpiration())
			.putObjectRequest(putObjectRequest)
			.build();

		try {
			PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(putObjectPresignRequest);
			return new GeneratePresignedUploadUrlResponse(
				originKey,
				presignedRequest.url().toString(),
				s3Properties.presignedUrlExpiration().toSeconds()
			);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED, exception);
		}
	}

	private User getRequiredUser(User authUser) {
		if (authUser == null || authUser.getId() == null) {
			throw new S3Exception(S3ErrorCode.USER_NOT_FOUND);
		}
		return userRepository.findWithProfileImageById(authUser.getId())
			.orElseThrow(() -> new S3Exception(S3ErrorCode.USER_NOT_FOUND));
	}

	private ProfileImage getOrCreateProfileImage(User user) {
		ProfileImage profileImage = user.getProfileImage();
		if (profileImage != null) {
			// 새 업로드는 새 시퀀스 아래에 다시 저장하므로 이전 origin/파생본은 먼저 정리한다.
			if (profileImage.hasAnyKey()) {
				s3ObjectStorageService.deleteAll(List.of(
					profileImage.getProfileOriginKey(),
					profileImage.getProfileThumbnailKey(),
					profileImage.getProfileThumbnailBlurKey(),
					profileImage.getProfileOriginBlurKey()
				));
			}
			return profileImage;
		}

		ProfileImage created = ProfileImage.create(user);
		return profileImageRepository.save(created);
	}

	private long getNextUploadSequence(ProfileImage profileImage) {
		long currentSequence = profileImage.getUploadSequence() == null ? 0L : profileImage.getUploadSequence();
		return currentSequence + 1;
	}

	private MediaType normalizeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			throw new S3Exception(S3ErrorCode.INVALID_CONTENT_TYPE);
		}

		try {
			MediaType mediaType = MediaType.parseMediaType(contentType.strip());
			if (!"image".equalsIgnoreCase(mediaType.getType())) {
				throw new S3Exception(S3ErrorCode.INVALID_CONTENT_TYPE);
			}
			return mediaType;
		} catch (InvalidMediaTypeException exception) {
			throw new S3Exception(S3ErrorCode.INVALID_CONTENT_TYPE);
		}
	}

	private String extractExtension(MediaType mediaType) {
		String subtype = mediaType.getSubtype().toLowerCase(Locale.ROOT);
		String normalizedSubtype = subtype.contains("+")
			? subtype.substring(0, subtype.indexOf('+'))
			: subtype;

		return switch (normalizedSubtype) {
			case "jpeg", "jpg", "pjpeg" -> ".jpg";
			case "png" -> ".png";
			case "gif" -> ".gif";
			case "webp" -> ".webp";
			case "bmp", "x-ms-bmp" -> ".bmp";
			case "svg" -> ".svg";
			case "heic" -> ".heic";
			case "heif" -> ".heif";
			case "vnd.microsoft.icon", "x-icon" -> ".ico";
			default -> "." + sanitizeExtension(normalizedSubtype);
		};
	}

	private String sanitizeExtension(String subtype) {
		String sanitized = subtype.replaceAll("[^a-zA-Z0-9]", "");
		if (sanitized.isBlank() || sanitized.length() > MAX_EXTENSION_LENGTH) {
			throw new S3Exception(S3ErrorCode.INVALID_CONTENT_TYPE);
		}
		return sanitized.toLowerCase(Locale.ROOT);
	}
}
