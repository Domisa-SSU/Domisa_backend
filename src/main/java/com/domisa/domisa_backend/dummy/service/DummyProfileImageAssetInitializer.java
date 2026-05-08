package com.domisa.domisa_backend.dummy.service;

import com.domisa.domisa_backend.global.s3.service.S3ObjectStorageService;
import com.domisa.domisa_backend.global.s3.service.S3ProfileImageKeyService;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
import com.domisa.domisa_backend.profileimage.service.ProfileImageProcessor;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class DummyProfileImageAssetInitializer implements ApplicationRunner {

	private static final int DUMMY_IMAGE_COUNT = 20;
	private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png"};

	private final ResourceLoader resourceLoader;
	private final S3ObjectStorageService s3ObjectStorageService;
	private final S3ProfileImageKeyService s3ProfileImageKeyService;
	private final ProfileImageProcessor profileImageProcessor;

	@Override
	public void run(ApplicationArguments args) {
		for (int index = 1; index <= DUMMY_IMAGE_COUNT; index++) {
			uploadDummyImageIfNeeded(index);
		}
	}

	private void uploadDummyImageIfNeeded(int index) {
		Resource resource = findDummyImageResource(index);
		if (resource == null) {
			log.debug("더미 프로필 이미지 리소스를 찾지 못했습니다. index={}", index);
			return;
		}

		DummyImageKeys keys = DummyImageKeys.of(index, s3ProfileImageKeyService);
		try (InputStream inputStream = resource.getInputStream()) {
			if (existsAll(keys)) {
				log.debug("더미 프로필 이미지가 이미 S3에 존재합니다. index={}", index);
				return;
			}

			byte[] originBytes = inputStream.readAllBytes();
			BufferedImage originImage = profileImageProcessor.read(originBytes);
			ProcessedProfileImageSet variants = profileImageProcessor.generateVariants(originImage);

			s3ObjectStorageService.uploadJpeg(keys.origin(), variants.origin());
			s3ObjectStorageService.uploadJpeg(keys.thumbnail(), variants.thumbnail());
			s3ObjectStorageService.uploadJpeg(keys.thumbnailBlur(), variants.thumbnailBlur());
			s3ObjectStorageService.uploadJpeg(keys.originBlur(), variants.originBlur());

			log.info("더미 프로필 이미지 파생본을 업로드했습니다. index={}", index);
		} catch (Exception exception) {
			log.warn(
				"더미 프로필 이미지 파생본 업로드에 실패했습니다. index={}, reason={}",
				index,
				exception.getMessage()
			);
		}
	}

	private Resource findDummyImageResource(int index) {
		for (String extension : SUPPORTED_EXTENSIONS) {
			Resource resource = resourceLoader.getResource(
				"classpath:dummy/profile-images/dummy" + index + extension
			);
			if (resource.exists()) {
				return resource;
			}
		}
		return null;
	}

	private boolean existsAll(DummyImageKeys keys) {
		try {
			return s3ObjectStorageService.exists(keys.origin())
				&& s3ObjectStorageService.exists(keys.thumbnail())
				&& s3ObjectStorageService.exists(keys.thumbnailBlur())
				&& s3ObjectStorageService.exists(keys.originBlur());
		} catch (Exception exception) {
			log.warn("기존 더미 프로필 이미지 자산을 확인하지 못했습니다. 업로드를 시도합니다. reason={}",
				exception.getMessage());
			return false;
		}
	}

	private record DummyImageKeys(
		String origin,
		String thumbnail,
		String thumbnailBlur,
		String originBlur
	) {

		private static DummyImageKeys of(int index, S3ProfileImageKeyService s3ProfileImageKeyService) {
			return new DummyImageKeys(
				s3ProfileImageKeyService.buildDummyOriginKey(index),
				s3ProfileImageKeyService.buildDummyThumbnailKey(index),
				s3ProfileImageKeyService.buildDummyThumbnailBlurKey(index),
				s3ProfileImageKeyService.buildDummyOriginBlurKey(index)
			);
		}
	}
}
