package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.profileimage.type.ProfileImageVariant;
import com.domisa.domisa_backend.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public class ProfileImageKeyResolver {

	private static final String PROFILE_DIRECTORY = "profiles";

	private final S3Properties s3Properties;

	public String buildOriginKey(User user, long uploadSequence, String extension) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.ORIGIN, extension);
	}

	public String buildThumbnailKey(User user, long uploadSequence) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.THUMBNAIL, ".jpg");
	}

	public String buildThumbnailBlurKey(User user, long uploadSequence) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.THUMBNAIL_BLUR, ".jpg");
	}

	public String buildOriginBlurKey(User user, long uploadSequence) {
		return buildVariantKey(user, uploadSequence, ProfileImageVariant.ORIGIN_BLUR, ".jpg");
	}

	private String buildVariantKey(User user, long uploadSequence, ProfileImageVariant variant, String extension) {
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
