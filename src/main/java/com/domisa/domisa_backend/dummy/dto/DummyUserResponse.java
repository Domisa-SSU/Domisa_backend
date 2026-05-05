package com.domisa.domisa_backend.dummy.dto;

import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.ContactType;
import java.util.List;
import java.util.Map;

public record DummyUserResponse(
	Long userId,
	String publicId,
	Long kakaoId,
	String nickname,
	Boolean gender,
	Long birthYear,
	AnimalProfile animalProfile,
	ContactType contactType,
	String contact,
	Long cookies,
	Boolean isRegistered,
	Boolean hasIntroduction,
	Boolean hasCard,
	List<String> nowShows,
	List<String> myFans,
	List<String> myTypes,
	List<String> myBlurs
) {

	public static DummyUserResponse from(User user, Map<Long, String> publicIdsByUserId) {
		return new DummyUserResponse(
			user.getId(),
			user.getPublicId(),
			user.getKakaoId(),
			user.getNickname(),
			user.getGender(),
			user.getBirthYear(),
			user.getAnimalProfile(),
			user.getContactType(),
			user.getContact(),
			user.getCookieBalance(),
			user.getIsRegistered(),
			user.hasIntroduction(),
			user.hasCard(),
			toPublicIds(user.getNowShows(), publicIdsByUserId),
			toPublicIds(user.getMyFans(), publicIdsByUserId),
			toPublicIds(user.getMyTypes(), publicIdsByUserId),
			toPublicIds(user.getMyBlurs(), publicIdsByUserId)
		);
	}

	private static List<String> toPublicIds(List<Long> userIds, Map<Long, String> publicIdsByUserId) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		return userIds.stream()
			.map(userId -> publicIdsByUserId.getOrDefault(userId, "#" + userId))
			.toList();
	}
}
