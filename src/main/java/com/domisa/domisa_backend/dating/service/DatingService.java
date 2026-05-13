package com.domisa.domisa_backend.dating.service;

import com.domisa.domisa_backend.auth.blacklist.repository.UserBlacklistRepository;
import com.domisa.domisa_backend.dating.dto.DatingMatchCountResponse;
import com.domisa.domisa_backend.dating.dto.DatingMatchListResponse;
import com.domisa.domisa_backend.dating.dto.DatingIntroductionLinkCreateRequest;
import com.domisa.domisa_backend.dating.dto.DatingIntroductionLinkCreateResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileDetailRequest;
import com.domisa.domisa_backend.dating.dto.DatingProfileResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileListResponse;
import com.domisa.domisa_backend.dating.dto.DatingRefreshTimeResponse;
import com.domisa.domisa_backend.dating.dto.UnblurProfileResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatingService {

	private static final Duration REFRESH_INTERVAL = Duration.ofHours(2);
	private static final int MAX_DATING_PROFILE_COUNT = 8;

	private final UserRepository userRepository;
	private final IntroductionRepository introductionRepository;
	private final S3ObjectUrlService s3ObjectUrlService;
	private final CookieTransactionRepository cookieTransactionRepository;
	private final NotificationService notificationService;
	private final UserBlacklistRepository userBlacklistRepository;

	@Transactional
	public DatingProfileListResponse getDatingProfiles(User authUser) {
		User requester = getRequiredUser(authUser);

		LocalDateTime now = LocalDateTime.now();
		if (canHaveNowShows(requester) && isRefreshDue(requester.getRefreshAvailableAt(), now)) {
			refreshNowShows(requester, now);
		}

		int freeLikeRemaining = getFreeLikeRemaining(requester);

		List<Long> nowShowIds = requester.getNowShows() == null
			? Collections.emptyList()
			: requester.getNowShows().stream()
				.limit(MAX_DATING_PROFILE_COUNT)
				.toList();
		Set<Long> unblurIds = requester.getMyBlurs() == null
			? Collections.emptySet()
			: new HashSet<>(requester.getMyBlurs());
		Set<Long> matchedIds = requester.getMyMatches() == null
			? Collections.emptySet()
			: new HashSet<>(requester.getMyMatches());
		LinkedHashMap<Long, User> usersById = getUsersById(nowShowIds);

		List<DatingProfileListResponse.ProfileSummary> profiles = nowShowIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new DatingProfileListResponse.ProfileSummary(
				targetUser.getPublicId(),
				unblurIds.contains(targetUser.getId()) || matchedIds.contains(targetUser.getId())
					? s3ObjectUrlService.getThumbnailPresignedUrl(targetUser.getProfileImage())
					: s3ObjectUrlService.getThumbnailBlurPresignedUrl(targetUser.getProfileImage())
			))
			.toList();

		return new DatingProfileListResponse(profiles.size(), freeLikeRemaining, profiles);
	}

	@Transactional
	public DatingProfileResponse getDatingProfile(User authUser, String publicId, DatingProfileDetailRequest request) {
		User requester = getRequiredUser(authUser);
		User targetUser = getDirectTargetUser(publicId);

		boolean isUnblurred = requester.getMyBlurs() != null && requester.getMyBlurs().contains(targetUser.getId());
		boolean isMatched = requester.getMyMatches() != null && requester.getMyMatches().contains(targetUser.getId());
		boolean isPaidUnblur = !isMatched && isUnblurred;
		boolean hasSentLike = requester.getMyTypes() != null && requester.getMyTypes().contains(targetUser.getId());
		boolean hasReceivedLike = requester.getMyFans() != null && requester.getMyFans().contains(targetUser.getId());
		int freeLikeRemaining = getFreeLikeRemaining(requester);

		// default: 쌍방매칭 or 블러 해제 → 프로필 정보 공개
		// a (NORMAL): 일반 조회 → 사진만 블러
		// b (FAN or received like): 받은 호감 상태 → 사진 + q3 + idealType 블러
		boolean isDefault = isMatched || isUnblurred;
		DatingProfileDetailRequest.ViewType viewType = request != null && request.viewType() != null
			? request.viewType()
			: DatingProfileDetailRequest.ViewType.NORMAL;

		boolean photoBlurred = !isDefault;
		boolean textBlurred = !isDefault
			&& (viewType == DatingProfileDetailRequest.ViewType.FAN || hasReceivedLike);

		String q3 = targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ3();
		String idealType = targetUser.getCard() == null ? null : targetUser.getCard().getIdealType();

		// Presigned URL 사용
		String profileUrl = photoBlurred
			? s3ObjectUrlService.getThumbnailBlurPresignedUrl(targetUser.getProfileImage())
			: s3ObjectUrlService.getThumbnailPresignedUrl(targetUser.getProfileImage());

		ContactDTO contact = isMatched
			? new ContactDTO(targetUser.getContactType(), targetUser.getContact())
			: null;

		return new DatingProfileResponse(
			targetUser.getPublicId(),
			targetUser.getNickname(),
			targetUser.getAge(),
			targetUser.getGender(),
			targetUser.getAnimalProfile(),
			profileUrl,
			targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ1(),
			targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ2(),
			textBlurred ? null : q3,
			textBlurred && q3 != null ? q3.length() : null,
			targetUser.getCard() == null ? null : targetUser.getCard().getDatingStyle(),
			textBlurred ? null : idealType,
			textBlurred && idealType != null ? idealType.length() : null,
			targetUser.getCard() == null ? null : targetUser.getCard().getMbti(),
			contact,
			photoBlurred,
			isPaidUnblur,
			hasSentLike,
			hasReceivedLike,
			isMatched,
			freeLikeRemaining
		);
	}

	@Transactional
	public DatingRefreshTimeResponse getDatingRefreshTime(User authUser) {
		User user = getRequiredUser(authUser);
		LocalDateTime now = LocalDateTime.now();

		if (canHaveNowShows(user) && isRefreshDue(user.getRefreshAvailableAt(), now)) {
			refreshNowShows(user, now);
		}

		return new DatingRefreshTimeResponse(user.getRefreshAvailableAt());
	}

	@Transactional
	public DatingIntroductionLinkCreateResponse createIntroductionLink(
		User authUser,
		DatingIntroductionLinkCreateRequest request
	) {
		User introducer = (authUser != null && authUser.getId() != null)
			? userRepository.findById(authUser.getId()).orElse(null)
			: null;

		String linkCode = generateUniqueLinkCode();

		introductionRepository.save(Introduction.create(
			request.q1(),
			request.q2(),
			request.q3(),
			introducer,
			linkCode
		));
		log.info("소개서 링크가 생성되었습니다. introducerId={}, linkCode={}",
			introducer == null ? null : introducer.getId(), linkCode);

		return new DatingIntroductionLinkCreateResponse(linkCode);
	}

	private LinkedHashMap<Long, User> getUsersById(List<Long> userIds) {
		// 소개팅 목록은 한 번에 조회해서 프로필 이미지 N+1을 줄인다.
		LinkedHashMap<Long, User> usersById = new LinkedHashMap<>();
		if (userIds.isEmpty()) {
			return usersById;
		}
		userRepository.findActiveAllByIdIn(userIds)
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

	private User getDirectTargetUser(String publicId) {
		User targetUser = userRepository.findDatingProfileByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
		if (userBlacklistRepository.existsByUserId(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.BLACKLISTED_TARGET_USER);
		}
		return targetUser;
	}

	@Transactional(readOnly = true)
	public DatingMatchListResponse getMatchList(User authUser) {
		User requester = getRequiredUser(authUser);

		if (requester.getMyMatches() == null || requester.getMyMatches().isEmpty()) {
			return new DatingMatchListResponse(0, Collections.emptyList());
		}

		List<Long> matchedIds = requester.getMyMatches();

		List<DatingMatchListResponse.MatchSummary> matches = userRepository.findActiveAllByIdIn(matchedIds).stream()
			.map(user -> new DatingMatchListResponse.MatchSummary(
				user.getPublicId(),
				user.getNickname(),
				user.getProfileImage() != null
					? s3ObjectUrlService.getThumbnailPresignedUrl(user.getProfileImage())
					: null,
				user.getContactType() != null ? user.getContactType().name() : null,
				user.getContact()
			))
			.toList();

		return new DatingMatchListResponse(matches.size(), matches);
	}

	@Transactional(readOnly = true)
	public DatingMatchCountResponse getMatchCount() {
		return new DatingMatchCountResponse(userRepository.countMutualMatches());
	}

	// 받은 호감 전용 블러 해제 — 쿠키 2개
	@Transactional
	public UnblurProfileResponse unblurReceivedLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = getDirectTargetUser(publicId);

		// 실제로 받은 호감인지 확인
		if (requester.getMyFans() == null || !requester.getMyFans().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}

		if (requester.getMyBlurs() != null && requester.getMyBlurs().contains(targetUser.getId())) {
			log.info("받은 호감 블러 해제를 건너뜁니다. reason=already_unblurred, requesterId={}, targetId={}",
				requester.getId(), targetUser.getId());
			return new UnblurProfileResponse(targetUser.getPublicId(), false, "이미 블러가 해제된 프로필입니다.");
		}

		if (requester.getCookies() == null || requester.getCookies() < 2) {
			throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
		}
		requester.setCookies(requester.getCookies() - 2);
		saveCookieUseTransaction(requester, 2, "받은 호감 블러 해제");

		if (requester.getMyBlurs() == null) {
			requester.setMyBlurs(new java.util.ArrayList<>());
		}
		requester.getMyBlurs().add(targetUser.getId());
		log.info("받은 호감 블러를 해제했습니다. requesterId={}, targetId={}, usedCookies=2, remainingCookies={}",
			requester.getId(), targetUser.getId(), requester.getCookieBalance());

		return new UnblurProfileResponse(targetUser.getPublicId(), false, "소개팅 카드 블러가 해제되었습니다.");
	}

	@Transactional
	public void matchReceivedLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = getDirectTargetUser(publicId);

		if (requester.getId().equals(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.CANNOT_LIKE_SELF);
		}

		if (requester.getMyFans() == null || !requester.getMyFans().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}

		if (requester.getMyBlurs() == null || !requester.getMyBlurs().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.NOT_UNBLURRED);
		}

		addMatch(requester, targetUser);
		notificationService.createNotification(NotificationType.MATCH, targetUser.getId(), requester.getId());
		log.info("소개팅 매칭을 수락했습니다. requesterId={}, targetId={}", requester.getId(), targetUser.getId());
	}

	@Transactional
	public void sendLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = getDirectTargetUser(publicId);

		if (requester.getId().equals(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.CANNOT_LIKE_SELF);
		}

		if (requester.getMyTypes() != null && requester.getMyTypes().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.ALREADY_LIKED);
		}
		if (requester.getMyMatches() != null && requester.getMyMatches().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.ALREADY_LIKED);
		}

		// 호감 보내기: 셔플마다 충전되는 무료 횟수를 먼저 쓰고, 없으면 쿠키 1개를 사용한다.
		boolean usedFreeLike = consumeFreeLikeAllowance(requester);
		if (!usedFreeLike) {
			if (requester.getCookies() == null || requester.getCookies() < 1) {
				throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
			}
			requester.setCookies(requester.getCookies() - 1);
			saveCookieUseTransaction(requester, 1, "호감 보내기");
		}

		if (requester.getMyTypes() == null) {
			requester.setMyTypes(new java.util.ArrayList<>());
		}
		addUnique(requester.getMyTypes(), targetUser.getId());

		if (targetUser.getMyFans() == null) {
			targetUser.setMyFans(new java.util.ArrayList<>());
		}
		addUnique(targetUser.getMyFans(), requester.getId());
		notificationService.createNotification(NotificationType.LIKE, targetUser.getId(), requester.getId());
		log.info(
			"호감을 보냈습니다. requesterId={}, targetId={}, usedFreeLike={}, usedCookies={}, freeLikeRemaining={}, remainingCookies={}",
			requester.getId(),
			targetUser.getId(),
			usedFreeLike,
			usedFreeLike ? 0 : 1,
			getFreeLikeRemaining(requester),
			requester.getCookieBalance()
		);
	}

	@Transactional
	public void shuffle(User authUser) {
		User requester = getRequiredUser(authUser);

		if (requester.getCookies() == null || requester.getCookies() < 2) {
			throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
		}

		if (!canHaveNowShows(requester)) {
			throw new GlobalException(GlobalErrorCode.PROFILE_NOT_COMPLETED);
		}

		refreshNowShows(requester, LocalDateTime.now());
		requester.setCookies(requester.getCookies() - 2);
		saveCookieUseTransaction(requester, 2, "소개팅 카드 셔플");
		log.info("소개팅 프로필을 셔플했습니다. userId={}, usedCookies=2, remainingCookies={}, nowShowCount={}",
			requester.getId(), requester.getCookieBalance(), requester.getNowShows().size());
	}

	@Transactional
	public void refreshNowShowsByAdmin(Long userId) {
		User user = userRepository.findWithProfileImageById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		if (!canHaveNowShows(user)) {
			throw new GlobalException(GlobalErrorCode.PROFILE_NOT_COMPLETED);
		}

		refreshNowShows(user, LocalDateTime.now());
		log.info("관리자가 소개팅 프로필 표시 목록을 갱신했습니다. userId={}, nowShowCount={}",
			user.getId(), user.getNowShows().size());
	}

	@Transactional
	public int refreshReadyNowShows() {
		LocalDateTime now = LocalDateTime.now();
		List<User> users = userRepository.findUsersReadyForNowShowRefresh(now);
		users.forEach(user -> refreshNowShows(user, now));
		if (!users.isEmpty()) {
			log.info("소개팅 프로필을 자동 갱신했습니다. userCount={}", users.size());
		}
		return users.size();
	}

	private void saveCookieUseTransaction(User user, int amount, String description) {
		cookieTransactionRepository.save(CookieTransaction.use(user, amount, description));
	}

	private void refreshNowShows(User user, LocalDateTime now) {
		user.setNowShows(new ArrayList<>(findRandomOppositeGenderUserIds(user)));
		user.setRefreshAvailableAt(nextRefreshAvailableAt(now));
		user.setFreeLikeCount(3);
		log.info("소개팅 표시 목록을 갱신했습니다. userId={}, nowShowCount={}, nextRefreshAvailableAt={}",
			user.getId(), user.getNowShows().size(), user.getRefreshAvailableAt());
	}

	private void addMatch(User user, User target) {
		if (user.getMyMatches() == null) {
			user.setMyMatches(new ArrayList<>());
		}
		if (target.getMyMatches() == null) {
			target.setMyMatches(new ArrayList<>());
		}
		if (user.getMyBlurs() == null) {
			user.setMyBlurs(new ArrayList<>());
		}

		addUnique(user.getMyMatches(), target.getId());
		addUnique(target.getMyMatches(), user.getId());
		addUnique(user.getMyBlurs(), target.getId());

		if (user.getMyFans() != null) {
			user.getMyFans().remove(target.getId());
		}
		if (target.getMyTypes() != null) {
			target.getMyTypes().remove(user.getId());
		}
	}

	private void addUnique(List<Long> values, Long targetUserId) {
		if (!values.contains(targetUserId)) {
			values.add(targetUserId);
		}
	}

	private boolean canHaveNowShows(User user) {
		return Boolean.TRUE.equals(user.getIsRegistered())
			&& hasIntroduction(user)
			&& user.hasCard();
	}

	private boolean hasIntroduction(User user) {
		return user.hasIntroduction() || introductionRepository.existsByParticipantId(user.getId());
	}

	private List<Long> findRandomOppositeGenderUserIds(User user) {
		if (user.getGender() == null) {
			return Collections.emptyList();
		}
		Set<Long> excludedUserIds = new HashSet<>();
		if (user.getMyFans() != null) {
			excludedUserIds.addAll(user.getMyFans());
		}
		if (user.getMyMatches() != null) {
			excludedUserIds.addAll(user.getMyMatches());
		}
		if (excludedUserIds.isEmpty()) {
			return userRepository.findRandomOppositeGenderUserIds(
				user.getId(),
				user.getGender(),
				MAX_DATING_PROFILE_COUNT
			);
		}
		return userRepository.findRandomOppositeGenderUserIdsExcluding(
			user.getId(),
			user.getGender(),
			excludedUserIds,
			MAX_DATING_PROFILE_COUNT
		);
	}

	private boolean isRefreshDue(LocalDateTime refreshAvailableAt, LocalDateTime now) {
		return refreshAvailableAt == null || !refreshAvailableAt.isAfter(now);
	}

	private LocalDateTime nextRefreshAvailableAt(LocalDateTime now) {
		return now.withNano(0).plus(REFRESH_INTERVAL);
	}

	private String generateUniqueLinkCode() {
		String linkCode;
		do {
			linkCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
		} while (introductionRepository.findByLinkCode(linkCode).isPresent());
		return linkCode;
	}

	private int getFreeLikeRemaining(User user) {
		return Math.max(0, user.getFreeLikeCount() == null ? 0 : user.getFreeLikeCount());
	}

	private boolean consumeFreeLikeAllowance(User user) {
		int remaining = getFreeLikeRemaining(user);
		if (remaining <= 0) {
			return false;
		}
		user.setFreeLikeCount(remaining - 1);
		return true;
	}

}
