package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.global.s3.dto.DeleteS3ObjectRequest;
import com.domisa.domisa_backend.global.s3.dto.DeleteS3ObjectResponse;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlRequest;
import com.domisa.domisa_backend.global.s3.dto.GeneratePresignedUploadUrlResponse;
import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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
	private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final DateTimeFormatter FILE_SEQUENCE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	private static final int MAX_EXTENSION_LENGTH = 20;

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;
	private final Clock clock;

	public S3PresignedUrlService(S3Client s3Client, S3Presigner s3Presigner, S3Properties s3Properties) {
		this(s3Client, s3Presigner, s3Properties, Clock.systemUTC());
	}

	S3PresignedUrlService(S3Client s3Client, S3Presigner s3Presigner, S3Properties s3Properties, Clock clock) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.s3Properties = s3Properties;
		this.clock = clock;
	}

	public GeneratePresignedUploadUrlResponse generatePresignedUploadUrl(GeneratePresignedUploadUrlRequest request) {
		MediaType mediaType = normalizeContentType(request.contentType());
		String contentType = mediaType.toString();
		String objectKey = buildObjectKey(request.userName(), mediaType);

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
			Instant expiresAt = Instant.now(clock).plus(s3Properties.presignedUrlExpiration());

			return new GeneratePresignedUploadUrlResponse(
				s3Properties.bucket(),
				objectKey,
				presignedRequest.url().toString(),
				HttpMethod.PUT.name(),
				contentType,
				expiresAt,
				Map.of("Content-Type", contentType)
			);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED, exception);
		}
	}

	public DeleteS3ObjectResponse deleteObject(DeleteS3ObjectRequest request) {
		String objectKey = normalizeObjectKey(request.objectKey());

		try {
			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(objectKey)
				.build());
			return new DeleteS3ObjectResponse(objectKey, true);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.OBJECT_DELETE_FAILED, exception);
		}
	}

	private String buildObjectKey(String userName, MediaType mediaType) {
		String normalizedConfiguredPrefix = normalizePrefix(s3Properties.uploadPrefix(), false);
		String profileFileName = buildProfileFileName(userName, mediaType);
		String datePath = LocalDate.now(clock).format(DATE_PATH_FORMATTER);

		List<String> segments = new ArrayList<>();
		if (!normalizedConfiguredPrefix.isBlank()) {
			segments.add(normalizedConfiguredPrefix);
		}
		segments.add(PROFILE_DIRECTORY);
		segments.add(datePath);
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

	private String normalizeObjectKey(String objectKey) {
		String normalized = objectKey == null ? "" : objectKey.strip().replace('\\', '/');
		if (normalized.isBlank() || normalized.contains("..") || normalized.startsWith("/")) {
			throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
		}
		return normalized.replaceAll("/+", "/");
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

	private String buildProfileFileName(String userName, MediaType mediaType) {
		String normalizedUserName = normalizeUserName(userName);
		String sequence = FILE_SEQUENCE_FORMATTER.format(Instant.now(clock).atZone(ZoneOffset.UTC));
		return normalizedUserName + "-profile-" + sequence + extractExtension(mediaType);
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
