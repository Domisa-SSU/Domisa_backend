package com.domisa.domisa_backend.sms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.domisa.domisa_backend.sms.client.BizgoClient;
import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

	@Mock
	private BizgoClient bizgoClient;

	@Test
	void sendBuildsSmsOmniRequest() {
		SmsService smsService = new SmsService(
			bizgoClient,
			new BizgoProperties("https://mars.ibapi.kr", "test-api-key", "02-1234-5678")
		);
		ArgumentCaptor<SmsRequest> requestCaptor = ArgumentCaptor.forClass(SmsRequest.class);

		smsService.send("010-1234-5678", " 안녕하세요 ");

		verify(bizgoClient).send(requestCaptor.capture());
		SmsRequest request = requestCaptor.getValue();
		assertThat(request.getDestinations()).hasSize(1);
		assertThat(request.getDestinations().getFirst().getTo()).isEqualTo("01012345678");
		assertThat(request.getMessageFlow()).hasSize(1);
		assertThat(request.getMessageFlow().getFirst().getSms().getFrom()).isEqualTo("0212345678");
		assertThat(request.getMessageFlow().getFirst().getSms().getText()).isEqualTo("안녕하세요");
	}

	@Test
	void sendAllSplitsDestinationsBy200() {
		SmsService smsService = new SmsService(
			bizgoClient,
			new BizgoProperties("https://mars.ibapi.kr", "test-api-key", "0212345678")
		);
		List<String> phones = IntStream.rangeClosed(1, 201)
			.mapToObj(number -> "0101234%04d".formatted(number))
			.toList();
		ArgumentCaptor<SmsRequest> requestCaptor = ArgumentCaptor.forClass(SmsRequest.class);

		smsService.sendAll(phones, "안녕하세요");

		verify(bizgoClient, times(2)).send(requestCaptor.capture());
		List<SmsRequest> requests = requestCaptor.getAllValues();
		assertThat(requests.get(0).getDestinations()).hasSize(200);
		assertThat(requests.get(1).getDestinations()).hasSize(1);
		assertThat(requests.get(0).getMessageFlow().getFirst().getSms().getText()).isEqualTo("안녕하세요");
		assertThat(requests.get(1).getMessageFlow().getFirst().getSms().getText()).isEqualTo("안녕하세요");
	}

	@Test
	void sendAllSkipsInvalidPhoneAndKeepsValidPhones() {
		SmsService smsService = new SmsService(
			bizgoClient,
			new BizgoProperties("https://mars.ibapi.kr", "test-api-key", "0212345678")
		);
		ArgumentCaptor<SmsRequest> requestCaptor = ArgumentCaptor.forClass(SmsRequest.class);

		smsService.sendAll(List.of("invalid", "010-2222-2222"), "안녕하세요");

		verify(bizgoClient).send(requestCaptor.capture());
		SmsRequest request = requestCaptor.getValue();
		assertThat(request.getDestinations()).hasSize(1);
		assertThat(request.getDestinations().getFirst().getTo()).isEqualTo("01022222222");
	}
}
