package com.domisa.domisa_backend.user.dto;

public record UserMeResponse(UserDto user, StatusDto status) {

	public record UserDto(
		Long userId,
		String nickname,
		Integer age,
		String gender,
		String profileImageUrl,
		Integer cookieCount,
		String referralCode
	) {
	}

	public record StatusDto(
		boolean isRegistered,
		boolean hasIntroduction,
		boolean isProfileCompleted
	) {
	}
}
