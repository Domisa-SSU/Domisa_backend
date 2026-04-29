package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
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

	private static final String PROFILE_DIRECTORY = "profiles";
	private static final String TEMP_DIRECTORY = "users/profile/temp";

	private final S3Properties s3Properties;

	public String buildSourceKey(User user, long uploadSequence, String extension) {
		// 프론트가 직접 올리는 원본은 temp 경로 아래에 둔다.
		List<String> segments = new ArrayList<>();
		String normalizedPrefix = normalizePrefix(s3Properties.uploadPrefix());
		if (!normalizedPrefix.isBlank()) {
			segments.add(normalizedPrefix);
		}
		segments.add(TEMP_DIRECTORY);
		segments.add(UUID.randomUUID() + extension);
		return String.join("/", segments);
	}

	public String buildThumbnailKey(User user, long uploadSequence) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.THUMBNAIL, ".jpg");
	}

	public String buildThumbnailBlurKey(User user, long uploadSequence) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.THUMBNAIL_BLUR, ".jpg");
	}

	public String buildDetailBlurKey(User user, long uploadSequence) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.DETAIL_BLUR, ".jpg");
	}

	private String buildVariantKey(User user, long uploadSequence, ProfileImageVariant variant, String extension) {
		// S3 경로 규칙은 여기서만 관리하고 프로필 이미지 로직은 키 조합을 몰라도 되게 한다.
		List<String> segments = new ArrayList<>();
		String normalizedPrefix = normalizePrefix(s3Properties.uploadPrefix());
		if (!normalizedPrefix.isBlank()) {
			segments.add(normalizedPrefix);
		}
		segments.add(PROFILE_DIRECTORY);
		segments.add(String.valueOf(user.getId()));
		segments.add(String.valueOf(uploadSequence));
		segments.add(variant.directory());
		segments.add(variant.fileName() + extension);
		return String.join("/", segments);
	}

	private String normalizePrefix(String prefix) {
		if (prefix == null || prefix.isBlank()) {
			return "";
		}
		return prefix.strip()
			.replace('\\', '/')
			.replaceAll("/+", "/")
			.replaceAll("^/+", "")
			.replaceAll("/+$", "");
	}
}
