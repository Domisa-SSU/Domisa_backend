package com.domisa.domisa_backend.dummy.service;

import com.domisa.domisa_backend.global.s3.service.S3ObjectStorageService;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
import com.domisa.domisa_backend.profileimage.service.ProfileImageProcessor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
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

	private static final int DUMMY_IMAGE_COUNT = 6;
	private static final String[] SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png"};

	private final ResourceLoader resourceLoader;
	private final S3ObjectStorageService s3ObjectStorageService;
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
			log.info("Dummy profile image resource not found. index={}", index);
			return;
		}

		DummyImageKeys keys = DummyImageKeys.of(index);
		try (InputStream inputStream = resource.getInputStream()) {
			if (existsAll(keys)) {
				log.info("Dummy profile image already exists in S3. index={}", index);
				return;
			}

			byte[] originBytes = inputStream.readAllBytes();
			BufferedImage originImage = profileImageProcessor.read(originBytes);
			ProcessedProfileImageSet variants = profileImageProcessor.generateVariants(originImage);

			s3ObjectStorageService.uploadJpeg(keys.origin(), writeJpeg(originImage));
			s3ObjectStorageService.uploadJpeg(keys.thumbnail(), variants.thumbnail());
			s3ObjectStorageService.uploadJpeg(keys.thumbnailBlur(), variants.thumbnailBlur());
			s3ObjectStorageService.uploadJpeg(keys.originBlur(), variants.originBlur());

			log.info("Uploaded dummy profile image variants. index={}", index);
		} catch (Exception exception) {
			log.warn(
				"Failed to upload dummy profile image variants. index={}, reason={}",
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
			log.warn("Could not verify existing dummy profile image assets. Upload will be attempted. reason={}",
				exception.getMessage());
			return false;
		}
	}

	private byte[] writeJpeg(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", outputStream);
			return outputStream.toByteArray();
		}
	}

	private record DummyImageKeys(
		String origin,
		String thumbnail,
		String thumbnailBlur,
		String originBlur
	) {

		private static DummyImageKeys of(int index) {
			String basePath = "dummy/profile-images/dummy" + index;
			return new DummyImageKeys(
				basePath + "/origin.jpg",
				basePath + "/thumbnail.jpg",
				basePath + "/thumbnail-blur.jpg",
				basePath + "/origin-blur.jpg"
			);
		}
	}
}
