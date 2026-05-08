package com.domisa.domisa_backend.payment.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;



@Component

public class OrderNumberGenerator {

	private static final DateTimeFormatter ORDER_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
	private static final char[] RANDOM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final int RANDOM_LENGTH = 9;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate(LocalDateTime orderDateTime) {
		return orderDateTime.format(ORDER_DATE_TIME_FORMATTER)
				+ "-"
				+ randomString();
	}

	private String randomString() {
		StringBuilder sb = new StringBuilder(OrderNumberGenerator.RANDOM_LENGTH);
		for (int i = 0; i < OrderNumberGenerator.RANDOM_LENGTH; i++) {
			sb.append(RANDOM_CHARS[secureRandom.nextInt(RANDOM_CHARS.length)]);
		}
		return sb.toString();
	}
}
