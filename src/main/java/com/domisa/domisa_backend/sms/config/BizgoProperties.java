package com.domisa.domisa_backend.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bizgo")
public record BizgoProperties(
	String baseUrl,
	String apiKey,
	String senderNumber
) {
}
