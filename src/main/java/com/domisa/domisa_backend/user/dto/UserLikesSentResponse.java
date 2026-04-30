package com.domisa.domisa_backend.user.dto;

import java.util.List;

public record UserLikesSentResponse(
	int myTypeNumber,
	List<UserType> myTypes
) {

	public record UserType(
		String userId,
		String profile
	) {
	}
}
