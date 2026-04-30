package com.domisa.domisa_backend.user.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.profile.dto.MyIntroductionResponse;
import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.dto.UserCookiesResponse;
import com.domisa.domisa_backend.user.dto.UserMeResponse;
import com.domisa.domisa_backend.user.dto.UserLikesReceivedResponse;
import com.domisa.domisa_backend.user.dto.UserLikesSentResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final S3ObjectUrlService s3ObjectUrlService;

	// 내 정보 조회(마이페이지용)
	@Transactional(readOnly = true)
	public UserMeResponse getMe(User authUser) {
		// 내 정보 조회는 프로필 이미지까지 함께 읽어서 응답한다.
		User user = getRequiredUser(authUser);

		return new UserMeResponse(
			user.getId(),
			user.getNickname(),
			user.getBirthYear(),
			user.getGender(),
			user.getAnimalProfile(),
			new ContactDTO(user.getContactType(), user.getContact()),
			user.getInviteCode()
		);
	}

	@Transactional(readOnly = true)
	public MyIntroductionResponse getMyIntroduction(User authUser) {
		User user = getRequiredUser(authUser);
		if (user.getIntroduction() == null) {
			return null;
		}
		return MyIntroductionResponse.from(user.getIntroduction());
	}

	@Transactional(readOnly = true)
	public UserCookiesResponse getMyCookies(User authUser) {
		User user = getRequiredUser(authUser);
		return new UserCookiesResponse(user.getCookies() == null ? 0L : user.getCookies());
	}

	@Transactional(readOnly = true)
	public UserLikesReceivedResponse getReceivedLikes(User authUser) {
		// 받은 좋아요 목록은 myFans 기준으로 만들고, myBlurs에 있으면 블러를 해제한 썸네일을 사용한다.
		User user = getRequiredUser(authUser);

		var fanIds = user.getMyFans() == null ? Collections.<Long>emptyList() : user.getMyFans();
		var unblurIds = user.getMyBlurs() == null ? Collections.<Long>emptySet() : new HashSet<>(user.getMyBlurs());
		var usersById = getUsersById(fanIds);

		var fans = fanIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new UserLikesReceivedResponse.UserFan(
				targetUser.getId(),
				unblurIds.contains(targetUser.getId())
					? s3ObjectUrlService.getThumbnailUrl(targetUser.getProfileImage())
					: s3ObjectUrlService.getThumbnailBlurUrl(targetUser.getProfileImage())
			))
			.toList();

		return new UserLikesReceivedResponse(fans.size(), fans);
	}

	@Transactional(readOnly = true)
	public UserLikesSentResponse getSentLikes(User authUser) {
		// 보낸 좋아요 목록은 항상 일반 썸네일로 응답한다.
		User user = getRequiredUser(authUser);

		var typeIds = user.getMyTypes() == null ? Collections.<Long>emptyList() : user.getMyTypes();
		var usersById = getUsersById(typeIds);

		var myTypes = typeIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new UserLikesSentResponse.UserType(
				targetUser.getId(),
				s3ObjectUrlService.getThumbnailUrl(targetUser.getProfileImage())
			))
			.toList();

		return new UserLikesSentResponse(myTypes.size(), myTypes);
	}

	private LinkedHashMap<Long, User> getUsersById(java.util.List<Long> userIds) {
		// 대상 유저를 한 번에 조회해서 목록 응답에서 N+1이 나지 않게 한다.
		LinkedHashMap<Long, User> usersById = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return usersById;
		}
		userRepository.findAllByIdIn(userIds)
			.forEach(user -> usersById.put(user.getId(), user));
		return usersById;
	}

	private User getRequiredUser(User authUser) {
		// 프로필 이미지까지 같이 읽어 이후 URL 생성에서 추가 조회를 줄인다.
		if (authUser == null || authUser.getId() == null) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}
		return userRepository.findWithProfileImageById(authUser.getId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
	}
}
