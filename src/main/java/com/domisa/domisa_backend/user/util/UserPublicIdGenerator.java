package com.domisa.domisa_backend.user.util;

import java.security.SecureRandom;

public final class UserPublicIdGenerator {

	private static final char[] CHARACTER_POOL =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	private static final int PUBLIC_ID_LENGTH = 16;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private UserPublicIdGenerator() {
	}

	public static String generate() {
		char[] publicId = new char[PUBLIC_ID_LENGTH];
		for (int index = 0; index < PUBLIC_ID_LENGTH; index++) {
			publicId[index] = CHARACTER_POOL[SECURE_RANDOM.nextInt(CHARACTER_POOL.length)];
		}
		return new String(publicId);
	}
}
