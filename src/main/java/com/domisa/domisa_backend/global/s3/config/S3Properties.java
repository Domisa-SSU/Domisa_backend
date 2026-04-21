package com.domisa.domisa_backend.global.s3.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationMin;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
	@NotBlank String bucket,
	@NotBlank String region,
	@NotBlank String accessKey,
	@NotBlank String secretKey,
	@NotBlank String uploadPrefix,
	@NotNull @DurationMin(seconds = 1) Duration presignedUrlExpiration
) {
}
