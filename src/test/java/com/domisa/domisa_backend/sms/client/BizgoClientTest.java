package com.domisa.domisa_backend.sms.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.Destination;
import com.domisa.domisa_backend.sms.dto.MessageFlow;
import com.domisa.domisa_backend.sms.dto.Sms;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class BizgoClientTest {

	@Test
	void sendUsesOmniSendApiPathAndApiKeyHeader() {
		AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
		WebClient webClient = WebClient.builder()
			.baseUrl("https://mars.ibapi.kr")
			.exchangeFunction(request -> {
				capturedRequest.set(request);
				return Mono.just(ClientResponse.create(HttpStatus.OK).body("{}").build());
			})
			.build();
		BizgoClient client = new BizgoClient(
			webClient,
			new BizgoProperties("https://mars.ibapi.kr", "test-api-key", "0212345678")
		);

		client.send(new SmsRequest(
			List.of(new Destination("01012345678")),
			List.of(new MessageFlow(new Sms("0212345678", "hello")))
		));

		ClientRequest request = capturedRequest.get();
		assertThat(request.method().name()).isEqualTo("POST");
		assertThat(request.url().getPath()).isEqualTo("/api/comm/v1/send/omni");
		assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("test-api-key");
	}
}
