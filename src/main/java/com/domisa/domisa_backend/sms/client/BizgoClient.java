package com.domisa.domisa_backend.sms.client;

import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BizgoClient {

	private static final String SEND_OMNI_PATH = "/api/comm/v1/send/omni";

	@Qualifier("bizgoWebClient")
	private final WebClient bizgoWebClient;
	private final BizgoProperties bizgoProperties;

	public String send(SmsRequest request) {
		log.info("Bizgo SMS API 요청을 시작합니다. path={}, apiKeyPresent={}, destinationCount={}",
			SEND_OMNI_PATH, StringUtils.hasText(bizgoProperties.apiKey()), request.getDestinations().size());
		String response = bizgoWebClient.post()
			.uri(SEND_OMNI_PATH)
			.header(HttpHeaders.AUTHORIZATION, requiredApiKey())
			.bodyValue(request)
			.exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
				.defaultIfEmpty("")
				.flatMap(body -> {
					if (clientResponse.statusCode().isError()) {
						log.warn("Bizgo SMS API 요청에 실패했습니다. status={}, body={}", clientResponse.statusCode(), body);
						return Mono.error(new IllegalStateException(
							"Bizgo SMS API 요청 실패. status=" + clientResponse.statusCode() + ", body=" + body
						));
					}
					log.info("Bizgo SMS API 요청에 성공했습니다. status={}, body={}", clientResponse.statusCode(), body);
					return Mono.just(body);
				}))
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
