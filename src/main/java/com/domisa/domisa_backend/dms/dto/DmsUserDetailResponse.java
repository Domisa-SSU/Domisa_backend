package com.domisa.domisa_backend.dms.dto;

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
	String profileImageUrl,
	IntroductionDetail introduction,
	CardDetail card
) {

	public boolean hasStudentCard() {
		return studentCardKey != null && !studentCardKey.isBlank();
	}

	public record IntroductionDetail(
		Long id,
		Long introducerId,
		String introducerPublicId,
		String introducerNickname,
		String linkCode,
		String q1,
		String q2,
		String q3
	) {
	}

	public record CardDetail(
		Long id,
		String mbti,
		String datingStyle,
		String idealType
	) {
	}
}
