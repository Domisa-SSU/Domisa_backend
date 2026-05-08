package com.domisa.domisa_backend.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PayActionProperties.class)
public class PayActionConfig {

	private static final String X_API_KEY = "x-api-key";
	private static final String X_MALL_ID = "x-mall-id";

	private final PayActionProperties properties;

	@Bean
	public RestClient payActionRestClient() {
		return RestClient.builder()
			.baseUrl(requiredValue(properties.getBaseUrl(), "PAYACTION_BASE_URL"))
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(X_API_KEY, requiredValue(properties.getApiKey(), "PAYACTION_API_KEY"))
			.defaultHeader(X_MALL_ID, requiredValue(properties.getMallId(), "PAYACTION_MALL_ID"))
			.build();
	}

	private String requiredValue(String value, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException(propertyName + " 환경변수가 설정되어 있지 않습니다.");
		}
		return value;
	}
}
