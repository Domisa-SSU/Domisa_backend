package com.domisa.domisa_backend.domain.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payaction")
public class PayActionProperties {

	private String baseUrl;
	private String apiKey;
	private String mallId;
	private String webhookKey;
	private Deposit deposit = new Deposit();

	@Getter
	@Setter
	public static class Deposit {
		private String bankName;
		private String bankCode;
		private String accountNumber;
		private String accountHolder;
	}
}
