package com.domisa.domisa_backend.sms.service;

import com.domisa.domisa_backend.sms.client.BizgoClient;
import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.Destination;
import com.domisa.domisa_backend.sms.dto.MessageFlow;
import com.domisa.domisa_backend.sms.dto.Sms;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SmsService {

	private static final int SMS_MAX_BYTES = 90;
	private static final String PHONE_NUMBER_PATTERN = "^01[016789]\\d{7,8}$";

	private final BizgoClient bizgoClient;
	private final BizgoProperties bizgoProperties;

	public void send(String phone, String message) {
		String normalizedPhone = normalizePhone(phone);
		String normalizedMessage = normalizeMessage(message);
		String senderNumber = normalizeSenderNumber(bizgoProperties.senderNumber());

		SmsRequest request = new SmsRequest(
			List.of(new Destination(normalizedPhone)),
			List.of(new MessageFlow(new Sms(senderNumber, normalizedMessage)))
		);

		bizgoClient.send(request);
	}

	private String normalizePhone(String phone) {
		if (!StringUtils.hasText(phone)) {
			throw new IllegalArgumentException("phone은 필수입니다.");
		}
		String normalizedPhone = phone.replaceAll("[^0-9]", "");
		if (!normalizedPhone.matches(PHONE_NUMBER_PATTERN)) {
			throw new IllegalArgumentException("phone 형식이 올바르지 않습니다.");
		}
		return normalizedPhone;
	}

	private String normalizeSenderNumber(String senderNumber) {
		if (!StringUtils.hasText(senderNumber)) {
			throw new IllegalStateException("BIZGO_SENDER_NUMBER 환경변수가 설정되어 있지 않습니다.");
		}
		String normalizedSenderNumber = senderNumber.replaceAll("[^0-9]", "");
		if (normalizedSenderNumber.isBlank()) {
			throw new IllegalStateException("BIZGO_SENDER_NUMBER 형식이 올바르지 않습니다.");
		}
		return normalizedSenderNumber;
	}

	private String normalizeMessage(String message) {
		if (!StringUtils.hasText(message)) {
			throw new IllegalArgumentException("message는 필수입니다.");
		}
		String normalizedMessage = message.strip();
		int byteLength = normalizedMessage.getBytes(StandardCharsets.UTF_8).length;
		if (byteLength > SMS_MAX_BYTES) {
			throw new IllegalArgumentException("SMS 메시지는 UTF-8 기준 90byte를 초과할 수 없습니다.");
		}
		return normalizedMessage;
	}
}
