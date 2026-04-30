package com.domisa.domisa_backend.global.s3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

	@Bean
	public S3Client s3Client(S3Properties s3Properties) {
		// 일반 S3 조회/업로드/삭제에 사용하는 클라이언트다.
		return S3Client.builder()
			.region(Region.of(s3Properties.region()))
			.build();
	}

	@Bean
	public S3Presigner s3Presigner(S3Properties s3Properties) {
		// 프론트 업로드용 presigned URL 발급에 사용한다.
		return S3Presigner.builder()
			.region(Region.of(s3Properties.region()))
			.build();
	}
}
