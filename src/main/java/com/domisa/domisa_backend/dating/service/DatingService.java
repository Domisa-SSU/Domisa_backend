package com.domisa.domisa_backend.dating.service;

import com.domisa.domisa_backend.dating.dto.DatingProfileResponse;
import com.domisa.domisa_backend.dating.dto.DatingRefreshTimeResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingService {

	private static final Duration REFRESH_INTERVAL = Duration.ofHours(2);

	private final UserRepository userRepository;
	private final S3ObjectUrlService s3ObjectUrlService;

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

	private User getRequiredUser(User authUser) {
		// 요청 유저는 프로필 이미지까지 같이 읽고 myBlurs, myTypes, nowShows를 기준으로 응답을 만든다.
		if (authUser == null || authUser.getId() == null) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}
		return userRepository.findWithProfileImageById(authUser.getId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
	}
}
