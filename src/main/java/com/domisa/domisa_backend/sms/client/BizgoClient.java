package com.domisa.domisa_backend.sms.client;

import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class BizgoClient {

	private static final String SEND_OMNI_PATH = "/api/comm/v1/send/omni";

	@Qualifier("bizgoWebClient")
	private final WebClient bizgoWebClient;
	private final BizgoProperties bizgoProperties;

	public String send(SmsRequest request) {
		String response = bizgoWebClient.post()
			.uri(SEND_OMNI_PATH)
			.header(HttpHeaders.AUTHORIZATION, requiredApiKey())
			.bodyValue(request)
			.retrieve()
			.bodyToMono(String.class)
			.block();

		return response == null ? "" : response;
	}

	private String requiredApiKey() {
		if (!StringUtils.hasText(bizgoProperties.apiKey())) {
			throw new IllegalStateException("BIZGO_API_KEY 환경변수가 설정되어 있지 않습니다.");
		}
		return bizgoProperties.apiKey().strip();
	}
}
