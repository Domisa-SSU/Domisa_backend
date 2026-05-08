package com.domisa.domisa_backend.profileimage.service;

import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import com.domisa.domisa_backend.profileimage.config.ProfileImageProcessingProperties;
import com.domisa.domisa_backend.profileimage.dto.ProcessedProfileImageSet;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
		// origin 한 장으로 최종 원본 JPG, 썸네일, 썸네일 블러, 오리진 블러 4종을 만든다.
		try {
			BufferedImage thumbnailImage = createThumbnail(originImage);
			BufferedImage originBlurImage = createOriginBlur(originImage);
			BufferedImage thumbnailBlurImage = blur(thumbnailImage, properties.getThumbnailBlurKernelSize());

			return new ProcessedProfileImageSet(
				writeJpeg(originImage),
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
			blurredImage = separableBoxBlurWithMirroredEdges(blurredImage, kernelSize);
		}
		return blurredImage;
	}

	private BufferedImage separableBoxBlurWithMirroredEdges(BufferedImage sourceImage, int kernelSize) {
		int radius = kernelSize / 2;
		int width = sourceImage.getWidth();
		int height = sourceImage.getHeight();
		int[] sourcePixels = sourceImage.getRGB(0, 0, width, height, null, 0, width);
		int[] horizontalPixels = new int[sourcePixels.length];
		int[] blurredPixels = new int[sourcePixels.length];

		blurHorizontal(sourcePixels, horizontalPixels, width, height, radius, kernelSize);
		blurVertical(horizontalPixels, blurredPixels, width, height, radius, kernelSize);

		BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		blurredImage.setRGB(0, 0, width, height, blurredPixels, 0, width);
		return blurredImage;
	}

	private void blurHorizontal(
		int[] sourcePixels,
		int[] targetPixels,
		int width,
		int height,
		int radius,
		int kernelSize
	) {
		for (int y = 0; y < height; y++) {
			int rowOffset = y * width;
			int redSum = 0;
			int greenSum = 0;
			int blueSum = 0;

			for (int offset = -radius; offset <= radius; offset++) {
				int pixel = sourcePixels[rowOffset + mirrorIndex(offset, width)];
				redSum += red(pixel);
				greenSum += green(pixel);
				blueSum += blue(pixel);
			}

			for (int x = 0; x < width; x++) {
				targetPixels[rowOffset + x] = rgb(redSum / kernelSize, greenSum / kernelSize, blueSum / kernelSize);

				int removeX = mirrorIndex(x - radius, width);
				int addX = mirrorIndex(x + radius + 1, width);
				int removePixel = sourcePixels[rowOffset + removeX];
				int addPixel = sourcePixels[rowOffset + addX];
				redSum += red(addPixel) - red(removePixel);
				greenSum += green(addPixel) - green(removePixel);
				blueSum += blue(addPixel) - blue(removePixel);
			}
		}
	}

	private void blurVertical(
		int[] sourcePixels,
		int[] targetPixels,
		int width,
		int height,
		int radius,
		int kernelSize
	) {
		for (int x = 0; x < width; x++) {
			int redSum = 0;
			int greenSum = 0;
			int blueSum = 0;

			for (int offset = -radius; offset <= radius; offset++) {
				int pixel = sourcePixels[mirrorIndex(offset, height) * width + x];
				redSum += red(pixel);
				greenSum += green(pixel);
				blueSum += blue(pixel);
			}

			for (int y = 0; y < height; y++) {
				targetPixels[y * width + x] = rgb(redSum / kernelSize, greenSum / kernelSize, blueSum / kernelSize);

				int removeY = mirrorIndex(y - radius, height);
				int addY = mirrorIndex(y + radius + 1, height);
				int removePixel = sourcePixels[removeY * width + x];
				int addPixel = sourcePixels[addY * width + x];
				redSum += red(addPixel) - red(removePixel);
				greenSum += green(addPixel) - green(removePixel);
				blueSum += blue(addPixel) - blue(removePixel);
			}
		}
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

	private int red(int pixel) {
		return pixel >> 16 & 0xff;
	}

	private int green(int pixel) {
		return pixel >> 8 & 0xff;
	}

	private int blue(int pixel) {
		return pixel & 0xff;
	}

	private int rgb(int red, int green, int blue) {
		return red << 16 | green << 8 | blue;
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
