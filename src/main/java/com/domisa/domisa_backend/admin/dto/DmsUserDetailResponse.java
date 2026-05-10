package com.domisa.domisa_backend.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DmsUserDetailResponse(
	Long id,
	String publicId,
	Long kakaoId,
	String name,
	String nickname,
	String gender,
	Long birthYear,
	Integer age,
	String animalProfile,
	Long cookies,
	String contactType,
	String contact,
	String inviteCode,
	Boolean isRegistered,
	Boolean hasIntroduction,
	Boolean isProfileCompleted,
	String notificationPhone,
	Integer freeLikeCount,
	String studentCardKey,
	Boolean isChecked,
	boolean blacklisted,
	LocalDateTime refreshAvailableAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<Long> myBlurs,
	List<Long> myFans,
	List<Long> myTypes,
	List<Long> myMatches,
	List<Long> nowShows,
	ProfileImageKeys profileImage
) {

	public boolean hasStudentCard() {
		return studentCardKey != null && !studentCardKey.isBlank();
	}

	public record ProfileImageKeys(
		String originKey,
		String originBlurKey,
		String thumbnailKey,
		String thumbnailBlurKey,
		String processingStatus,
		Integer retryCount,
		String lastError
	) {
	}
}
