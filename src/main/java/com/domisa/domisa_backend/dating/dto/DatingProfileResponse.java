package com.domisa.domisa_backend.dating.dto;

import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.Mbti;

public record DatingProfileResponse(
	String userId,
	String nickName,
	Integer age,
	AnimalProfile animalProfile,
	String profile,
	String q1,
	String q2,
	String q3,
	Integer q3Length,        // 블러 시 q3 문자열 길이
	String datingStyle,
	String idealType,
	Integer idealTypeLength, // 블러 시 idealType 문자열 길이
	Mbti mbti,
	ContactDTO contact,
	boolean isBlurred,
	boolean hasSentLike,
	boolean hasReceivedLike, // 나에게 호감 보낸 사람인지
	boolean isMatched,       // 이미 쌍방 매칭된 사람인지
	int freeLikeRemaining
) {
}
