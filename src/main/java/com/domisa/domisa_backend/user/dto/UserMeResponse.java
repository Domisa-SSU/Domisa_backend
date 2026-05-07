package com.domisa.domisa_backend.user.dto;

import com.domisa.domisa_backend.user.type.AnimalProfile;

public record UserMeResponse(
		String publicId,
		String nickname,
		Long birthYear,
		Boolean gender,
		AnimalProfile animalProfile,
		StatusDto status
) {
	public record StatusDto(
			boolean isRegistered,
			boolean hasIntroduction,
			boolean isProfileCompleted
	) {}
}
