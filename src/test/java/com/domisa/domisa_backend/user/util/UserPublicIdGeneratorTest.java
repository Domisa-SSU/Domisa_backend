package com.domisa.domisa_backend.user.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserPublicIdGeneratorTest {

	@Test
	void generateCreatesSixteenCharacterBase62Id() {
		String publicId = UserPublicIdGenerator.generate();

		assertThat(publicId).hasSize(16);
		assertThat(publicId).matches("^[A-Za-z0-9]{16}$");
	}
}
