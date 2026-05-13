package com.domisa.domisa_backend.dms.dto;

public record DmsUserStatsResponse(
	long totalUsers,
	long checkedUsers,
	long uncheckedUsers,
	long maleUsers,
	long femaleUsers,
	long todayJoinedUsers
) {
}
