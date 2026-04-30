package com.domisa.domisa_backend.dating.dto;

import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.Mbti;

public record DatingProfileResponse(
	String publicId,
	String nickName,
	Integer age,
	AnimalProfile animalProfile,
	String profile,
	String q1,
	String q2,
	String q3,
	String datingStyle,
	String idealType,
	Mbti mbti,
	ContactDTO contact,
	boolean isBlurred,
	boolean hasSentLike
) {
}
