package com.domisa.domisa_backend.dating.dto;

import java.util.List;

public record DatingProfileListResponse(
	int profileNum,
	List<ProfileSummary> profiles
) {
	public record ProfileSummary(
		String userId,
		String profile
	) {
	}
}
