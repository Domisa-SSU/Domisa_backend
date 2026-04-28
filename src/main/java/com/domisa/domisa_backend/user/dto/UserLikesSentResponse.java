package com.domisa.domisa_backend.user.dto;

import java.util.List;

public record UserLikesSentResponse(
	int myTypeNumber,
	List<LikeUserItem> myTypes
) {

	public record LikeUserItem(
		Long userId,
		String profile
	) {
	}
}
