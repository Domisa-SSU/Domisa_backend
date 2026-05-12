package com.domisa.domisa_backend.payment.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class BillingNameGenerator {

	private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int CODE_LENGTH = 8;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate() {
		StringBuilder builder = new StringBuilder(CODE_LENGTH);
		for (int index = 0; index < CODE_LENGTH; index++) {
			builder.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
		}
		return builder.toString();
	}
}
