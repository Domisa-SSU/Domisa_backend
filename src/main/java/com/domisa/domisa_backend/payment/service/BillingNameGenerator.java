package com.domisa.domisa_backend.payment.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class BillingNameGenerator {

	private static final String PREFIX = "입금";
	private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int CODE_LENGTH = 6;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate() {
		StringBuilder builder = new StringBuilder(PREFIX);
		for (int index = 0; index < CODE_LENGTH; index++) {
			builder.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
		}
		return builder.toString();
	}
}
