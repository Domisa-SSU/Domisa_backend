package com.domisa.domisa_backend.dating.dto;

import java.util.List;

public record DatingProfilesResponse(
	int profileNum,
	List<DatingListProfile> profiles
) {
	public record DatingListProfile(
		Long userId,
		String profile
	) {
	}
}
