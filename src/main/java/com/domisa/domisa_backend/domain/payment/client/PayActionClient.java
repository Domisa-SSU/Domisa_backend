package com.domisa.domisa_backend.domain.payment.client;

import com.domisa.domisa_backend.domain.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.domain.payment.dto.PayActionCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PayActionClient {

	private final RestClient payActionRestClient;

	public PayActionCreateOrderResponse createOrder(PayActionCreateOrderRequest request) {
		return payActionRestClient.post()
			.uri("/order")
			.body(request)
			.retrieve()
			.body(PayActionCreateOrderResponse.class);
	}
}
