package com.domisa.domisa_backend.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DmsUserListResponse(
	DmsUserStatsResponse stats,
	List<UserRow> users,
	String checked,
	String status
) {

	public record UserRow(
		Long id,
		String publicId,
		String name,
		String nickname,
		String gender,
		Long birthYear,
		Long cookies,
		Boolean isChecked,
		boolean blacklisted,
		String studentCardKey,
		LocalDateTime createdAt
	) {
		public boolean hasStudentCard() {
			return studentCardKey != null && !studentCardKey.isBlank();
		}
	}
}
