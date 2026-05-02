package com.domisa.domisa_backend.payment.client;

import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionOrderExcludeRequest;
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

	public PayActionCreateOrderResponse excludeOrder(PayActionOrderExcludeRequest request) {
		return payActionRestClient.post()
			.uri("/order-exclude")
			.body(request)
			.retrieve()
			.body(PayActionCreateOrderResponse.class);
	}
}
