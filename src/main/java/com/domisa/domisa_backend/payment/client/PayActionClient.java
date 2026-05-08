package com.domisa.domisa_backend.payment.client;

import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionOrderExcludeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayActionClient {

	private final RestClient payActionRestClient;

	public PayActionCreateOrderResponse createOrder(PayActionCreateOrderRequest request) {
		log.info("페이액션 주문 등록 API 호출을 시작합니다. path=/order, orderNumber={}, orderAmount={}",
			request.orderNumber(), request.orderAmount());
		PayActionCreateOrderResponse response = payActionRestClient.post()
			.uri("/order")
			.body(request)
			.retrieve()
			.body(PayActionCreateOrderResponse.class);
		log.info("페이액션 주문 등록 API 응답을 수신했습니다. orderNumber={}, success={}",
			request.orderNumber(), response != null && response.isSuccess());
		return response;
	}

	public PayActionCreateOrderResponse excludeOrder(PayActionOrderExcludeRequest request) {
		log.info("페이액션 주문 매칭 제외 API 호출을 시작합니다. path=/order-exclude, orderNumber={}",
			request.orderNumber());
		PayActionCreateOrderResponse response = payActionRestClient.post()
			.uri("/order-exclude")
			.body(request)
			.retrieve()
			.body(PayActionCreateOrderResponse.class);
		log.info("페이액션 주문 매칭 제외 API 응답을 수신했습니다. orderNumber={}, success={}",
			request.orderNumber(), response != null && response.isSuccess());
		return response;
	}
}
