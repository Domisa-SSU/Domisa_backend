package com.domisa.domisa_backend.dating.service;

import com.domisa.domisa_backend.dating.dto.DatingProfileResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileListResponse;
import com.domisa.domisa_backend.dating.dto.DatingRefreshTimeResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingService {

	private static final Duration REFRESH_INTERVAL = Duration.ofHours(2);
	private static final int MAX_DATING_PROFILE_COUNT = 8;

	private final UserRepository userRepository;
	private final S3ObjectUrlService s3ObjectUrlService;

	@Transactional(readOnly = true)
	public DatingProfileListResponse getDatingProfiles(User authUser) {
		User requester = getRequiredUser(authUser);

		List<Long> nowShowIds = requester.getNowShows() == null
			? Collections.emptyList()
			: requester.getNowShows().stream().limit(MAX_DATING_PROFILE_COUNT).toList();
		Set<Long> unblurIds = requester.getMyBlurs() == null
			? Collections.emptySet()
			: new HashSet<>(requester.getMyBlurs());
		LinkedHashMap<Long, User> usersById = getUsersById(nowShowIds);

		List<DatingProfileListResponse.ProfileSummary> profiles = nowShowIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new DatingProfileListResponse.ProfileSummary(
				targetUser.getId(),
				unblurIds.contains(targetUser.getId())
					? s3ObjectUrlService.getThumbnailUrl(targetUser.getProfileImage())
					: s3ObjectUrlService.getThumbnailBlurUrl(targetUser.getProfileImage())
			))
			.toList();

		return new DatingProfileListResponse(profiles.size(), profiles);
	}

	@Transactional(readOnly = true)
	public DatingProfileResponse getDatingProfile(User authUser, Long userId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findDatingProfileById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		boolean isBlurred = requester.getMyBlurs() == null || !requester.getMyBlurs().contains(userId);
		boolean hasSentLike = requester.getMyTypes() != null && requester.getMyTypes().contains(userId);

		String profileUrl = isBlurred
			? s3ObjectUrlService.getOriginBlurUrl(targetUser.getProfileImage())
			: s3ObjectUrlService.getProfileImageUrl(targetUser.getProfileImage());
		ContactDTO contact = isBlurred
			? null
			: new ContactDTO(targetUser.getContactType(), targetUser.getContact());

		return new DatingProfileResponse(
			targetUser.getId(),
			targetUser.getNickname(),
			targetUser.getAge(),
			targetUser.getAnimalProfile(),
			profileUrl,
			targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ1(),
			targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ2(),
			targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ3(),
			targetUser.getCard() == null ? null : targetUser.getCard().getDatingStyle(),
			targetUser.getCard() == null ? null : targetUser.getCard().getIdealType(),
			targetUser.getCard() == null ? null : targetUser.getCard().getMbti(),
			contact,
			isBlurred,
			hasSentLike
		);
	}

	@Transactional(readOnly = true)
	public DatingRefreshTimeResponse getDatingRefreshTime(User authUser) {
		User user = getRequiredUser(authUser);
		LocalDateTime refreshAvailableAt = user.getRefreshAt() == null
			? null
			: user.getRefreshAt().plus(REFRESH_INTERVAL);
		boolean canRefresh = refreshAvailableAt == null || !LocalDateTime.now().isBefore(refreshAvailableAt);
		return new DatingRefreshTimeResponse(refreshAvailableAt, canRefresh);
	}

	private LinkedHashMap<Long, User> getUsersById(List<Long> userIds) {
		// 소개팅 목록은 한 번에 조회해서 프로필 이미지 N+1을 줄인다.
		LinkedHashMap<Long, User> usersById = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return usersById;
		}
		userRepository.findAllByIdIn(userIds)
			.forEach(user -> usersById.put(user.getId(), user));
		return usersById;
	}

	private User getRequiredUser(User authUser) {
		// 요청 유저는 프로필 이미지까지 같이 읽고 myBlurs, myTypes, nowShows를 기준으로 응답을 만든다.
		if (authUser == null || authUser.getId() == null) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}
		return userRepository.findWithProfileImageById(authUser.getId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
	}
}
