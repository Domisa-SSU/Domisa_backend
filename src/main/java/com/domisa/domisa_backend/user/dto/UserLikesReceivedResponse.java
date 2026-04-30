package com.domisa.domisa_backend.user.dto;

import java.util.List;

public record UserLikesReceivedResponse(
	int myFanNumber,
	List<UserFan> myFans
) {

	public record UserFan(
		String userId,
		String profile
	) {
	}
}
