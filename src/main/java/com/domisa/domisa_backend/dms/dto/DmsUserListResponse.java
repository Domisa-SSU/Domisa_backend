package com.domisa.domisa_backend.dms.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DmsUserListResponse(
	DmsUserStatsResponse stats,
	List<UserRow> users,
	String checked,
	String status,
	String gender,
	String birthYearSort,
	String keyword,
	boolean completedOnly,
	int page,
	int size,
	int totalPages,
	long totalElements,
	boolean hasPrevious,
	boolean hasNext,
	boolean hasPreviousGroup,
	boolean hasNextGroup,
	int previousGroupPage,
	int nextGroupPage,
	List<Integer> pageNumbers
) {

	public record UserRow(
		Long id,
		String publicId,
		String nickname,
		String gender,
		Long birthYear,
		Long cookies,
		Boolean isRegistered,
		Boolean hasIntroduction,
		Boolean isProfileCompleted,
		Boolean isChecked,
		boolean blacklisted,
		boolean hasProfileImage,
		LocalDateTime createdAt
	) {
	}
}
