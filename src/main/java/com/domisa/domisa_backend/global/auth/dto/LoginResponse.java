package com.domisa.domisa_backend.global.auth.dto;

public record LoginResponse(StatusDto status) {

	public record StatusDto(
		boolean isRegistered,
		boolean hasIntroduction,
		boolean isProfileCompleted
	) {
	}
}
