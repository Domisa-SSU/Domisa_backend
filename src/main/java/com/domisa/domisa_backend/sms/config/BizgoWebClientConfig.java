package com.domisa.domisa_backend.sms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(BizgoProperties.class)
public class BizgoWebClientConfig {

	@Bean
	public WebClient bizgoWebClient(BizgoProperties properties) {
		return WebClient.builder()
			.baseUrl(requiredValue(properties.baseUrl(), "BIZGO_BASE_URL"))
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}

	private String requiredValue(String value, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException(propertyName + " 환경변수가 설정되어 있지 않습니다.");
		}
		return value;
	}
}
