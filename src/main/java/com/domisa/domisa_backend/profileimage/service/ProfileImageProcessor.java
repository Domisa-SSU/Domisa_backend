package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageProcessor {

	private final ProfileImageProcessingProperties properties;

	public ProcessedProfileImageSet generateVariants(BufferedImage originImage) {
		// origin 한 장으로 썸네일, 썸네일 블러, 오리진 블러 3종을 만든다.
		try {
			BufferedImage thumbnailImage = createThumbnail(originImage);
			BufferedImage originBlurImage = createOriginBlur(originImage);
			BufferedImage thumbnailBlurImage = blur(thumbnailImage, properties.getThumbnailBlurKernelSize());

			return new ProcessedProfileImageSet(
				writeJpeg(thumbnailImage),
				writeJpeg(thumbnailBlurImage),
				writeJpeg(originBlurImage)
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
		// thumbnailSize 값 기준으로 비율을 유지한 채 전체 크기만 줄인다.
		return toRgb(Thumbnails.of(originImage)
			.size(properties.getThumbnailSize(), properties.getThumbnailSize())
			.keepAspectRatio(true)
			.asBufferedImage());
	}

	private BufferedImage createOriginBlur(BufferedImage originImage) throws IOException {
		// originMaxSize 값으로 긴 변만 제한하고, originBlurKernelSize로 블러를 적용한다.
		BufferedImage resized = toRgb(Thumbnails.of(originImage)
			.size(properties.getOriginMaxSize(), properties.getOriginMaxSize())
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

	private byte[] writeJpeg(BufferedImage image) throws IOException {
		// 별도 품질 보정은 하지 않고 기본 JPG 인코딩만 수행한다.
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Thumbnails.of(image)
				.scale(1.0)
				.outputFormat("jpg")
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
