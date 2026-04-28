package com.domisa.domisa_backend.profileimage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.profile-image.processing")
public class ProfileImageProcessingProperties {

	private int thumbnailSize = 300;
	private int detailMaxSize = 1080;
	private int thumbnailBlurKernelSize = 13;
	private int detailBlurKernelSize = 17;
	private double thumbnailJpegQuality = 0.85d;
	private double detailBlurJpegQuality = 0.9d;
	private int batchSize = 10;
	private int maxRetryCount = 5;

	public int getThumbnailSize() {
		return thumbnailSize;
	}

	public void setThumbnailSize(int thumbnailSize) {
		this.thumbnailSize = thumbnailSize;
	}

	public int getDetailMaxSize() {
		return detailMaxSize;
	}

	public void setDetailMaxSize(int detailMaxSize) {
		this.detailMaxSize = detailMaxSize;
	}

	public int getThumbnailBlurKernelSize() {
		return thumbnailBlurKernelSize;
	}

	public void setThumbnailBlurKernelSize(int thumbnailBlurKernelSize) {
		this.thumbnailBlurKernelSize = thumbnailBlurKernelSize;
	}

	public int getDetailBlurKernelSize() {
		return detailBlurKernelSize;
	}

	public void setDetailBlurKernelSize(int detailBlurKernelSize) {
		this.detailBlurKernelSize = detailBlurKernelSize;
	}

	public double getThumbnailJpegQuality() {
		return thumbnailJpegQuality;
	}

	public void setThumbnailJpegQuality(double thumbnailJpegQuality) {
		this.thumbnailJpegQuality = thumbnailJpegQuality;
	}

	public double getDetailBlurJpegQuality() {
		return detailBlurJpegQuality;
	}

	public void setDetailBlurJpegQuality(double detailBlurJpegQuality) {
		this.detailBlurJpegQuality = detailBlurJpegQuality;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int getMaxRetryCount() {
		return maxRetryCount;
	}

	public void setMaxRetryCount(int maxRetryCount) {
		this.maxRetryCount = maxRetryCount;
	}
}
