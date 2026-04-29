package com.domisa.domisa_backend.user.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
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

	@Transactional(readOnly = true)
	public UserMeResponse getMe(User authUser) {
		ProfileImage profileImage = authUser.getProfileImage();

		return new UserMeResponse(
			new UserMeResponse.UserDto(
				authUser.getId(),
				authUser.getNickname(),
				authUser.getAge(),
				authUser.getGenderDisplay(),
				s3ObjectUrlService.getProfileImageUrl(profileImage),
				Math.toIntExact(authUser.getCookies()),
				authUser.getInviteCode()
			),
			new UserMeResponse.StatusDto(
					authUser.getIsRegistered(),
					authUser.hasIntroduction(),
					authUser.hasCard()
			)
		);
	}

	@Transactional(readOnly = true)
	public UserLikesReceivedResponse getReceivedLikes(User authUser) {
		User user = getRequiredUser(authUser);

		var fanIds = user.getMyFans() == null ? Collections.<Long>emptyList() : user.getMyFans();
		var blurIds = user.getMyBlurs() == null ? Collections.<Long>emptySet() : new HashSet<>(user.getMyBlurs());
		var usersById = getUsersById(fanIds);

		var fans = fanIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new UserLikesReceivedResponse.LikeUserItem(
				targetUser.getId(),
				blurIds.contains(targetUser.getId())
					? s3ObjectUrlService.getThumbnailBlurUrl(targetUser.getProfileImage())
					: s3ObjectUrlService.getThumbnailUrl(targetUser.getProfileImage())
			))
			.toList();

		return new UserLikesReceivedResponse(fans.size(), fans);
	}

	@Transactional(readOnly = true)
	public UserLikesSentResponse getSentLikes(User authUser) {
		User user = getRequiredUser(authUser);

		var typeIds = user.getMyTypes() == null ? Collections.<Long>emptyList() : user.getMyTypes();
		var usersById = getUsersById(typeIds);

		var myTypes = typeIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new UserLikesSentResponse.LikeUserItem(
				targetUser.getId(),
				s3ObjectUrlService.getThumbnailUrl(targetUser.getProfileImage())
			))
			.toList();

		return new UserLikesSentResponse(myTypes.size(), myTypes);
	}

	private LinkedHashMap<Long, User> getUsersById(java.util.List<Long> userIds) {
		LinkedHashMap<Long, User> usersById = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return usersById;
		}

		userRepository.findAllByIdIn(userIds)
			.forEach(user -> usersById.put(user.getId(), user));
		return usersById;
	}

	private User getRequiredUser(User authUser) {
		if (authUser == null || authUser.getId() == null) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}

		return userRepository.findWithProfileImageById(authUser.getId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
	}
}
