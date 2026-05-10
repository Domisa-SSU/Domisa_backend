package com.domisa.domisa_backend.admin.dto;

public record DmsUserStatsResponse(
	long totalUsers,
	long checkedUsers,
	long uncheckedUsers,
	long maleUsers,
	long femaleUsers,
	long todayJoinedUsers
) {
}
