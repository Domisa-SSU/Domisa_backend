package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.domain.user.repository.UserRepository;
import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.global.s3.dto.DeleteS3ObjectResponse;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlResponse;
import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3PresignedUrlService {

	private static final String PROFILE_DIRECTORY = "users/profile";
	private static final int MAX_EXTENSION_LENGTH = 20;

	private final UserRepository userRepository;
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;

	public S3PresignedUrlService(
		UserRepository userRepository,
		S3Client s3Client,
		S3Presigner s3Presigner,
		S3Properties s3Properties
	) {
		this.userRepository = userRepository;
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.s3Properties = s3Properties;
	}

	@Transactional
	public GeneratePresignedUploadUrlResponse createProfileImageUploadUrl(User authUser, GeneratePresignedUploadUrlRequest request) {
		User user = getUser(authUser);
		if (user.hasProfileImage()) {
			throw new S3Exception(S3ErrorCode.PROFILE_IMAGE_ALREADY_EXISTS);
		}
		return issueProfileImageUploadUrl(user, request);
	}

	@Transactional
	public GeneratePresignedUploadUrlResponse updateProfileImageUploadUrl(User authUser, GeneratePresignedUploadUrlRequest request) {
		User user = getUser(authUser);
		if (!user.hasProfileImage()) {
			throw new S3Exception(S3ErrorCode.PROFILE_IMAGE_NOT_FOUND);
		}
		return issueProfileImageUploadUrl(user, request);
	}

	@Transactional
	public DeleteS3ObjectResponse deleteProfileImage(User authUser) {
		User user = getUser(authUser);
		if (!user.hasProfileImage()) {
			throw new S3Exception(S3ErrorCode.PROFILE_IMAGE_NOT_FOUND);
		}

		String objectKey = user.getProfileImageObjectKey();
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(objectKey)
				.build());
			user.setProfileImageObjectKey(null);
			return new DeleteS3ObjectResponse(user.getId(), objectKey, true);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.OBJECT_DELETE_FAILED, exception);
		}
	}

	private GeneratePresignedUploadUrlResponse issueProfileImageUploadUrl(User user, GeneratePresignedUploadUrlRequest request) {
		MediaType mediaType = normalizeContentType(request.contentType());
		String contentType = mediaType.toString();
		long currentSequence = user.getProfileImageSequence() == null ? 0L : user.getProfileImageSequence();
		long uploadSequence = currentSequence + 1;
		user.setProfileImageSequence(uploadSequence);
		String objectKey = buildObjectKey(user, uploadSequence, mediaType);

		user.setProfileImageObjectKey(objectKey);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(s3Properties.bucket())
			.key(objectKey)
			.contentType(contentType)
			.build();

		PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(s3Properties.presignedUrlExpiration())
			.putObjectRequest(putObjectRequest)
			.build();

		try {
			PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(putObjectPresignRequest);
			return new GeneratePresignedUploadUrlResponse(
				objectKey,
				presignedRequest.url().toString()
			);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED, exception);
		}
	}

	private User getUser(User authUser) {
		if (authUser == null || authUser.getId() == null) {
			throw new S3Exception(S3ErrorCode.USER_NOT_FOUND);
		}
		return userRepository.findById(authUser.getId())
			.orElseThrow(() -> new S3Exception(S3ErrorCode.USER_NOT_FOUND));
	}

	private String buildObjectKey(User user, long uploadSequence, MediaType mediaType) {
		String normalizedConfiguredPrefix = normalizePrefix(s3Properties.uploadPrefix(), false);
		String profileFileName = buildProfileFileName(user, uploadSequence, mediaType);

		List<String> segments = new ArrayList<>();
		if (!normalizedConfiguredPrefix.isBlank()) {
			segments.add(normalizedConfiguredPrefix);
		}
		segments.add(PROFILE_DIRECTORY);
		segments.add(profileFileName);

		return String.join("/", segments);
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

	private String normalizePrefix(String prefix, boolean required) {
		if (prefix == null || prefix.isBlank()) {
			if (!required) {
				return "";
			}
			throw new S3Exception(S3ErrorCode.INVALID_PREFIX);
		}

		String normalized = prefix.strip().replace('\\', '/');
		if (normalized.contains("..")) {
			throw new S3Exception(S3ErrorCode.INVALID_PREFIX);
		}

		normalized = normalized.replaceAll("/+", "/");
		normalized = trimSlashes(normalized);
		if (normalized.isBlank()) {
			if (!required) {
				return "";
			}
			throw new S3Exception(S3ErrorCode.INVALID_PREFIX);
		}

		String[] rawSegments = normalized.split("/");
		List<String> sanitizedSegments = new ArrayList<>();
		for (String rawSegment : rawSegments) {
			String sanitizedSegment = rawSegment.strip()
				.replaceAll("[^a-zA-Z0-9_-]", "-")
				.replaceAll("-{2,}", "-");

			if (sanitizedSegment.isBlank()) {
				throw new S3Exception(S3ErrorCode.INVALID_PREFIX);
			}
			sanitizedSegments.add(sanitizedSegment);
		}
		return String.join("/", sanitizedSegments);
	}

	private String buildProfileFileName(User user, long uploadSequence, MediaType mediaType) {
		String normalizedName = normalizeUserName(user.getName());
		String normalizedNickname = normalizeUserName(user.getNickname());
		return normalizedName + "-" + normalizedNickname + "-profile-" + uploadSequence + extractExtension(mediaType);
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

	private String normalizeUserName(String userName) {
		if (userName == null || userName.isBlank()) {
			throw new S3Exception(S3ErrorCode.INVALID_USER_NAME);
		}

		String normalized = Normalizer.normalize(userName.strip(), Normalizer.Form.NFKC)
			.replaceAll("\\s+", "-")
			.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}_-]", "-")
			.replaceAll("-{2,}", "-");

		normalized = trimDelimiters(normalized);
		if (normalized.isBlank()) {
			throw new S3Exception(S3ErrorCode.INVALID_USER_NAME);
		}
		return normalized;
	}

	private String trimSlashes(String value) {
		int start = 0;
		int end = value.length();

		while (start < end && value.charAt(start) == '/') {
			start++;
		}
		while (end > start && value.charAt(end - 1) == '/') {
			end--;
		}
		return value.substring(start, end);
	}

	private String trimDelimiters(String value) {
		int start = 0;
		int end = value.length();

		while (start < end && (value.charAt(start) == '-' || value.charAt(start) == '_')) {
			start++;
		}
		while (end > start && (value.charAt(end - 1) == '-' || value.charAt(end - 1) == '_')) {
			end--;
		}
		return value.substring(start, end);
	}
}
