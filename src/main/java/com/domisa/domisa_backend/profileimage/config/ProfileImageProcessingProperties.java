package com.domisa.domisa_backend.profileimage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.profile-image.processing")
public class ProfileImageProcessingProperties {

	// 파생본 생성 규칙과 재시도 설정을 한곳에서 관리한다.
	// 썸네일은 비율을 유지한 채 긴 변 기준으로 이 값까지 줄인다.
	private int thumbnailSize = 300;
	// 오리진 블러 이미지는 긴 변 기준으로 이 값까지 줄인다.
	private int detailMaxSize = 1080;
	// 썸네일 블러 강도다. 홀수 커널 크기이며 커질수록 더 흐려진다.
	private int thumbnailBlurKernelSize = 13;
	// 오리진 블러 강도다. 홀수 커널 크기이며 커질수록 더 흐려진다.
	private int originBlurKernelSize = 17;
	// 한 번에 처리할 프로필 이미지 건수다.
	private int batchSize = 10;
	// 실패 시 최대 재시도 횟수다.
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

	public int getOriginBlurKernelSize() {
		return originBlurKernelSize;
	}

	public void setOriginBlurKernelSize(int originBlurKernelSize) {
		this.originBlurKernelSize = originBlurKernelSize;
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
