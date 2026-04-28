package com.domisa.domisa_backend.user.dto;

import java.util.List;

public record UserLikesReceivedResponse(
	int myFanNumber,
	List<LikeUserItem> myFans
) {

	public record LikeUserItem(
		Long userId,
		String profile
	) {
	}
}
