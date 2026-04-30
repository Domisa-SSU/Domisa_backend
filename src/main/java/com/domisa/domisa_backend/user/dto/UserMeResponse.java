package com.domisa.domisa_backend.user.dto;

import com.domisa.domisa_backend.user.type.AnimalProfile;

public record UserMeResponse(
		Long userId,
		String nickname,
		Long birthYear,
		Boolean gender,
		AnimalProfile animalProfile,
		ContactDTO contact,
		String myInviteCode,     // 내 추천인 코드
		String profileImageUrl
) {}

