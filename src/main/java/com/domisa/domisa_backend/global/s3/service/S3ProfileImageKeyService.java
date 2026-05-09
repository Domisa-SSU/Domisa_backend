package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.profileimage.type.ProfileImageVariant;
import com.domisa.domisa_backend.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class S3ProfileImageKeyService {

	private static final String USER_DIRECTORY = "users";
	private static final String DUMMY_DIRECTORY = "dummy";
	private static final String PROFILE_IMAGES_DIRECTORY = "profile-images";
	private static final String TEMP_DIRECTORY = "temp";

	public String buildUploadKey(User user, String extension) {
		List<String> segments = new ArrayList<>();
		segments.add(USER_DIRECTORY);
		segments.add(PROFILE_IMAGES_DIRECTORY);
		segments.add(user.getPublicId());
		segments.add(TEMP_DIRECTORY);
		segments.add(UUID.randomUUID() + extension);
		return String.join("/", segments);
	}

	public boolean isUploadKey(String objectKey) {
		return objectKey != null && objectKey.contains("/" + TEMP_DIRECTORY + "/");
	}

	public String buildOriginKey(User user) {
		return buildVariantKey(USER_DIRECTORY, user.getPublicId(), ProfileImageVariant.ORIGIN);
	}

	public String buildThumbnailKey(User user) {
		return buildVariantKey(USER_DIRECTORY, user.getPublicId(), ProfileImageVariant.THUMBNAIL);
	}

	public String buildThumbnailBlurKey(User user) {
		return buildVariantKey(USER_DIRECTORY, user.getPublicId(), ProfileImageVariant.THUMBNAIL_BLUR);
	}

	public String buildOriginBlurKey(User user) {
		return buildVariantKey(USER_DIRECTORY, user.getPublicId(), ProfileImageVariant.ORIGIN_BLUR);
	}

	public String buildDummyOriginKey(int index) {
		return buildVariantKey(DUMMY_DIRECTORY, buildDummyProfileDirectory(index), ProfileImageVariant.ORIGIN);
	}

	public String buildDummyThumbnailKey(int index) {
		return buildVariantKey(DUMMY_DIRECTORY, buildDummyProfileDirectory(index), ProfileImageVariant.THUMBNAIL);
	}

	public String buildDummyThumbnailBlurKey(int index) {
		return buildVariantKey(DUMMY_DIRECTORY, buildDummyProfileDirectory(index), ProfileImageVariant.THUMBNAIL_BLUR);
	}

	public String buildDummyOriginBlurKey(int index) {
		return buildVariantKey(DUMMY_DIRECTORY, buildDummyProfileDirectory(index), ProfileImageVariant.ORIGIN_BLUR);
	}

	private String buildVariantKey(
		String rootDirectory,
		String profileDirectory,
		ProfileImageVariant variant
	) {
		// S3 경로 규칙은 여기서만 관리하고 프로필 이미지 로직은 키 조합을 몰라도 되게 한다.
		List<String> segments = new ArrayList<>();
		segments.add(rootDirectory);
		segments.add(PROFILE_IMAGES_DIRECTORY);
		segments.add(profileDirectory);
		segments.add(fileName(variant));
		return String.join("/", segments);
	}

	private String buildDummyProfileDirectory(int index) {
		return "dummy" + index;
	}

	private String fileName(ProfileImageVariant variant) {
		return switch (variant) {
			case ORIGIN -> "origin.jpg";
			case THUMBNAIL -> "thumbnail.jpg";
			case THUMBNAIL_BLUR -> "thumbnail-blur.jpg";
			case ORIGIN_BLUR -> "origin-blur.jpg";
		};
	}
}
