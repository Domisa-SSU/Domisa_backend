package com.domisa.domisa_backend.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PayActionProperties.class)
public class PayActionConfig {

	private final PayActionProperties properties;

	@Bean
	public RestClient payActionRestClient() {
		return RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader("x-api-key", properties.getApiKey())
			.defaultHeader("x-mall-id", properties.getMallId())
			.build();
	}
}
