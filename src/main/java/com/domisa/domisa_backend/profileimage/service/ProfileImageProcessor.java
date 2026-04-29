package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageProcessor {

	private final ProfileImageProcessingProperties properties;

	public ProcessedProfileImageSet generateVariants(BufferedImage originImage) {
		// origin 한 장으로 썸네일/썸네일 블러/오리진 블러를 만든다.
		try {
			BufferedImage thumbnailImage = createThumbnail(originImage);
			BufferedImage originBlurImage = createOriginBlur(originImage);
			BufferedImage thumbnailBlurImage = blur(thumbnailImage, properties.getThumbnailBlurKernelSize());

			return new ProcessedProfileImageSet(
				writeJpeg(thumbnailImage, properties.getThumbnailJpegQuality()),
				writeJpeg(thumbnailBlurImage, properties.getThumbnailJpegQuality()),
				writeJpeg(originBlurImage, properties.getOriginBlurJpegQuality())
			);
		} catch (IOException exception) {
			throw new S3Exception(S3ErrorCode.IMAGE_PROCESSING_FAILED, exception);
		}
	}

	public BufferedImage read(byte[] originBytes) {
		// 후속 처리의 일관성을 위해 RGB 이미지로 정규화한다.
		try {
			BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(originBytes));
			if (image == null) {
				throw new S3Exception(S3ErrorCode.IMAGE_PROCESSING_FAILED, "지원하지 않는 이미지 형식입니다.");
			}
			return toRgb(image);
		} catch (IOException exception) {
			throw new S3Exception(S3ErrorCode.IMAGE_PROCESSING_FAILED, exception);
		}
	}

	private BufferedImage createThumbnail(BufferedImage originImage) throws IOException {
		return toRgb(Thumbnails.of(originImage)
			.size(properties.getThumbnailSize(), properties.getThumbnailSize())
			.crop(Positions.CENTER)
			.asBufferedImage());
	}

	private BufferedImage createOriginBlur(BufferedImage originImage) throws IOException {
		// 오리진 블러는 원본 비율을 유지하고 긴 변만 제한한다.
		BufferedImage resized = toRgb(Thumbnails.of(originImage)
			.size(properties.getDetailMaxSize(), properties.getDetailMaxSize())
			.keepAspectRatio(true)
			.asBufferedImage());
		return blur(resized, properties.getOriginBlurKernelSize());
	}

	private BufferedImage blur(BufferedImage sourceImage, int kernelSize) {
		validateKernelSize(kernelSize);
		// 정규화된 박스 커널을 사용해 같은 파이프라인에서 블러 강도만 조절한다.
		float[] kernelData = new float[kernelSize * kernelSize];
		float value = 1.0f / (kernelSize * kernelSize);
		for (int index = 0; index < kernelData.length; index++) {
			kernelData[index] = value;
		}

		ConvolveOp convolveOp = new ConvolveOp(
			new Kernel(kernelSize, kernelSize, kernelData),
			ConvolveOp.EDGE_NO_OP,
			null
		);
		return toRgb(convolveOp.filter(sourceImage, null));
	}

	private byte[] writeJpeg(BufferedImage image, double quality) throws IOException {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Thumbnails.of(image)
				.scale(1.0)
				.outputFormat("jpg")
				.outputQuality(quality)
				.toOutputStream(outputStream);
			return outputStream.toByteArray();
		}
	}

	private BufferedImage toRgb(BufferedImage image) {
		if (image.getType() == BufferedImage.TYPE_INT_RGB) {
			return image;
		}
		BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgbImage.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return rgbImage;
	}

	private void validateKernelSize(int kernelSize) {
		if (kernelSize < 3 || kernelSize % 2 == 0) {
			throw new S3Exception(S3ErrorCode.IMAGE_PROCESSING_FAILED, "블러 커널 크기는 3 이상의 홀수여야 합니다.");
		}
	}
}
