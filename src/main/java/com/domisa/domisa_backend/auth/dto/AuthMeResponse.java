package com.domisa.domisa_backend.auth.dto;

public record AuthMeResponse(
	Long userId,
	Integer cookies,
	StatusDto status
) {
	public record StatusDto(
		boolean isRegistered,
		boolean hasIntroduction,
		boolean isProfileCompleted
	) {}
}
