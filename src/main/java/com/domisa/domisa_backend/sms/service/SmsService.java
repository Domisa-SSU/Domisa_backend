package com.domisa.domisa_backend.sms.service;

import com.domisa.domisa_backend.sms.client.BizgoClient;
import com.domisa.domisa_backend.sms.config.BizgoProperties;
import com.domisa.domisa_backend.sms.dto.Destination;
import com.domisa.domisa_backend.sms.dto.MessageFlow;
import com.domisa.domisa_backend.sms.dto.Sms;
import com.domisa.domisa_backend.sms.dto.SmsRequest;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

	private static final int MAX_DESTINATION_COUNT = 200;
	private static final String PHONE_NUMBER_PATTERN = "^01[016789]\\d{7,8}$";

	private final BizgoClient bizgoClient;
	private final BizgoProperties bizgoProperties;
	private final UserRepository userRepository;

	public void send(String phone, String message) {
		sendAll(List.of(phone), message);
	}

	public void sendAll(Collection<String> phones, String message) {
		if (phones == null || phones.isEmpty()) {
			throw new IllegalArgumentException("phones는 필수입니다.");
		}

		List<String> normalizedPhones = phones.stream()
			.map(this::normalizePhone)
			.toList();
		Set<String> blacklistedPhones = findBlacklistedPhones(normalizedPhones);
		normalizedPhones = normalizedPhones.stream()
			.filter(phone -> isNotBlacklistedPhone(phone, blacklistedPhones))
			.toList();
		String normalizedMessage = normalizeMessage(message);
		String senderNumber = normalizeSenderNumber(bizgoProperties.senderNumber());
		log.info("SMS 발송 요청을 준비했습니다. totalCount={}, sender={}, messageLength={}, phones={}",
			normalizedPhones.size(), maskPhone(senderNumber), normalizedMessage.length(), maskPhones(normalizedPhones));
		if (normalizedPhones.isEmpty()) {
			log.info("SMS 발송 대상 전화번호가 없습니다.");
			return;
		}

		for (int start = 0; start < normalizedPhones.size(); start += MAX_DESTINATION_COUNT) {
			int end = Math.min(start + MAX_DESTINATION_COUNT, normalizedPhones.size());
			int batchNumber = (start / MAX_DESTINATION_COUNT) + 1;
			sendBatch(normalizedPhones.subList(start, end), senderNumber, normalizedMessage, batchNumber);
		}
	}

	private Set<String> findBlacklistedPhones(List<String> phones) {
		if (phones.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(userRepository.findBlacklistedNormalizedNotificationPhones(phones));
	}

	private boolean isNotBlacklistedPhone(String phone, Set<String> blacklistedPhones) {
		if (!blacklistedPhones.contains(phone)) {
			return true;
		}
		log.info("SMS 발송 대상에서 블랙리스트 유저 전화번호를 제외했습니다. phone={}", maskPhone(phone));
		return false;
	}

	private void sendBatch(List<String> phones, String senderNumber, String message, int batchNumber) {
		SmsRequest request = new SmsRequest(
			phones.stream()
				.map(Destination::new)
				.toList(),
			List.of(new MessageFlow(new Sms(senderNumber, message)))
		);

		log.info("SMS 배치 발송을 요청합니다. batch={}, count={}, phones={}", batchNumber, phones.size(), maskPhones(phones));
		String response = bizgoClient.send(request);
		log.info("SMS 배치 발송 응답을 받았습니다. batch={}, count={}, response={}", batchNumber, phones.size(), response);
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
		return message.strip();
	}

	private List<String> maskPhones(List<String> phones) {
		return phones.stream()
			.map(this::maskPhone)
			.toList();
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return "";
		}
		String normalizedPhone = phone.replaceAll("[^0-9]", "");
		if (normalizedPhone.length() <= 4) {
			return "****";
		}
		return "***-****-" + normalizedPhone.substring(normalizedPhone.length() - 4);
	}
}
