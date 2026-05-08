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

	private static final int BLUR_PASS_COUNT = 3;

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
		BufferedImage blurredImage = toRgb(sourceImage);
		for (int pass = 0; pass < BLUR_PASS_COUNT; pass++) {
			blurredImage = boxBlurWithMirroredEdges(blurredImage, kernelSize);
		}
		return blurredImage;
	}

	private BufferedImage boxBlurWithMirroredEdges(BufferedImage sourceImage, int kernelSize) {
		int radius = kernelSize / 2;
		BufferedImage paddedImage = mirrorPad(sourceImage, radius);

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
		BufferedImage blurredPaddedImage = convolveOp.filter(paddedImage, null);
		return cropCenter(blurredPaddedImage, radius, sourceImage.getWidth(), sourceImage.getHeight());
	}

	private BufferedImage mirrorPad(BufferedImage sourceImage, int padding) {
		int width = sourceImage.getWidth();
		int height = sourceImage.getHeight();
		BufferedImage paddedImage = new BufferedImage(
			width + padding * 2,
			height + padding * 2,
			BufferedImage.TYPE_INT_RGB
		);

		for (int y = 0; y < paddedImage.getHeight(); y++) {
			int sourceY = mirrorIndex(y - padding, height);
			for (int x = 0; x < paddedImage.getWidth(); x++) {
				int sourceX = mirrorIndex(x - padding, width);
				paddedImage.setRGB(x, y, sourceImage.getRGB(sourceX, sourceY));
			}
		}
		return paddedImage;
	}

	private int mirrorIndex(int index, int size) {
		if (size <= 1) {
			return 0;
		}
		while (index < 0 || index >= size) {
			if (index < 0) {
				index = -index - 1;
			} else {
				index = size * 2 - index - 1;
			}
		}
		return index;
	}

	private BufferedImage cropCenter(BufferedImage image, int padding, int width, int height) {
		BufferedImage croppedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = croppedImage.createGraphics();
		graphics.drawImage(
			image,
			0,
			0,
			width,
			height,
			padding,
			padding,
			padding + width,
			padding + height,
			null
		);
		graphics.dispose();
		return croppedImage;
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
