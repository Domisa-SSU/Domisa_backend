package com.domisa.domisa_backend.domain.user.command;

public record RegisterProfileCommand(
	String nickname,
	Boolean gender,
	Long birthYear,
	String animalProfile,
	String contact,
	String inviteCode
) {
}
