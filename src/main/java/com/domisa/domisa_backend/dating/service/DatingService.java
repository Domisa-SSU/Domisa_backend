package com.domisa.domisa_backend.dating.service;

import com.domisa.domisa_backend.cookie.repository.CookieWalletRepository;
import com.domisa.domisa_backend.dating.dto.DatingMatchCountResponse;
import com.domisa.domisa_backend.dating.dto.DatingIntroductionLinkCreateRequest;
import com.domisa.domisa_backend.dating.dto.DatingIntroductionLinkCreateResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileListResponse;
import com.domisa.domisa_backend.dating.dto.DatingRefreshTimeResponse;
import com.domisa.domisa_backend.dating.dto.UnblurProfileResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingService {

	private static final Duration REFRESH_INTERVAL = Duration.ofHours(2);
	private static final int MAX_DATING_PROFILE_COUNT = 8;

	private final UserRepository userRepository;
	private final IntroductionRepository introductionRepository;
	private final S3ObjectUrlService s3ObjectUrlService;
	private final CookieWalletRepository cookieWalletRepository;

	@Transactional
	public DatingProfileListResponse getDatingProfiles(User authUser) {
		User requester = getRequiredUser(authUser);

		int freeBlurRemaining = getFreeBlurRemaining(requester);

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
				targetUser.getPublicId(),
				unblurIds.contains(targetUser.getId())
					? s3ObjectUrlService.getThumbnailUrl(targetUser.getProfileImage())
					: s3ObjectUrlService.getThumbnailBlurUrl(targetUser.getProfileImage())
			))
			.toList();

		return new DatingProfileListResponse(profiles.size(), freeBlurRemaining, profiles);
	}

	@Transactional(readOnly = true)
	public DatingProfileResponse getDatingProfile(User authUser, String userId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findDatingProfileByPublicId(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		boolean isBlurred = requester.getMyBlurs() == null || !requester.getMyBlurs().contains(targetUser.getId());
		boolean hasSentLike = requester.getMyTypes() != null && requester.getMyTypes().contains(targetUser.getId());

		String profileUrl = isBlurred
			? s3ObjectUrlService.getOriginBlurUrl(targetUser.getProfileImage())
			: s3ObjectUrlService.getProfileImageUrl(targetUser.getProfileImage());
		ContactDTO contact = isBlurred
			? null
			: new ContactDTO(targetUser.getContactType(), targetUser.getContact());

		return new DatingProfileResponse(
			targetUser.getPublicId(),
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

	@Transactional
	public DatingIntroductionLinkCreateResponse createIntroductionLink(
		User authUser,
		DatingIntroductionLinkCreateRequest request
	) {
		User introducer = getRequiredUser(authUser);
		String linkCode = generateUniqueLinkCode();

		introductionRepository.save(Introduction.create(
			request.q1(),
			request.q2(),
			request.q3(),
			introducer,
			linkCode
		));

		return new DatingIntroductionLinkCreateResponse(linkCode);
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

	@Transactional(readOnly = true)
	public DatingMatchCountResponse getMatchCount() {
		return new DatingMatchCountResponse(userRepository.countMutualMatches());
	}

	@Transactional
	public UnblurProfileResponse unblurProfile(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		if (requester.getId().equals(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.CANNOT_LIKE_SELF);
		}

		if (requester.getMyBlurs() != null && requester.getMyBlurs().contains(targetUser.getId())) {
			return new UnblurProfileResponse(targetUser.getPublicId(), false, "이미 블러가 해제된 프로필입니다.");
		}

		int freeBlurRemaining = getFreeBlurRemaining(requester);
		if (freeBlurRemaining > 0) {
			requester.setFreeBlurCount(requester.getFreeBlurCount() + 1);
			if (requester.getFreeBlurResetAt() == null) {
				requester.setFreeBlurResetAt(LocalDateTime.now());
			}
		} else {
			if (requester.getCookies() == null || requester.getCookies() < 1) {
				throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
			}
			requester.setCookies(requester.getCookies() - 1);
		}

		if (requester.getMyBlurs() == null) {
			requester.setMyBlurs(new java.util.ArrayList<>());
		}
		requester.getMyBlurs().add(targetUser.getId());

		return new UnblurProfileResponse(targetUser.getPublicId(), false, "프로필 블러가 해제되었습니다.");
	}

	@Transactional
	public void sendLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		if (requester.getId().equals(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.CANNOT_LIKE_SELF);
		}

		// 블러 해제한 상대에게만 호감 보내기 가능
		if (requester.getMyBlurs() == null || !requester.getMyBlurs().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.NOT_UNBLURRED);
		}

		if (requester.getMyTypes() != null && requester.getMyTypes().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.ALREADY_LIKED);
		}

		if (requester.getMyTypes() == null) {
			requester.setMyTypes(new java.util.ArrayList<>());
		}
		requester.getMyTypes().add(targetUser.getId());

		if (targetUser.getMyFans() == null) {
			targetUser.setMyFans(new java.util.ArrayList<>());
		}
		targetUser.getMyFans().add(requester.getId());
	}

	@Transactional
	public void shuffle(User authUser) {
		User requester = getRequiredUser(authUser);

		if (requester.getCookies() == null || requester.getCookies() < 3) {
			throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
		}

		List<Long> randomIds = userRepository.findRandomUserIds(requester.getId(), MAX_DATING_PROFILE_COUNT);
		requester.setNowShows(new java.util.ArrayList<>(randomIds));
		requester.setCookies(requester.getCookies() - 3);
		requester.setRefreshAt(LocalDateTime.now());
	}

	private String generateUniqueLinkCode() {
		String linkCode;
		do {
			linkCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
		} while (introductionRepository.findByLinkCode(linkCode).isPresent());
		return linkCode;
	}

	private int getFreeBlurRemaining(User user) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime resetAt = user.getFreeBlurResetAt();

		if (resetAt == null || resetAt.toLocalDate().isBefore(now.toLocalDate())) {
			user.setFreeBlurCount(0);
			user.setFreeBlurResetAt(now);
		}
		return Math.max(0, 3 - user.getFreeBlurCount());
	}
}
