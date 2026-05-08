package com.domisa.domisa_backend.dating.service;
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
	private final CookieTransactionRepository cookieTransactionRepository;

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
		LinkedHashMap<Long, User> usersById = getUsersById(nowShowIds);

		List<DatingProfileListResponse.ProfileSummary> profiles = nowShowIds.stream()
			.map(usersById::get)
			.filter(targetUser -> targetUser != null)
			.map(targetUser -> new DatingProfileListResponse.ProfileSummary(
				targetUser.getPublicId(),
				unblurIds.contains(targetUser.getId())
					? s3ObjectUrlService.getThumbnailPresignedUrl(targetUser.getProfileImage())
					: s3ObjectUrlService.getThumbnailBlurPresignedUrl(targetUser.getProfileImage())
			))
			.toList();

		return new DatingProfileListResponse(profiles.size(), freeLikeRemaining, profiles);
	}

	@Transactional
	public DatingProfileResponse getDatingProfile(User authUser, String publicId, DatingProfileDetailRequest request) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findDatingProfileByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		boolean isPaidUnblur = requester.getMyBlurs() != null && requester.getMyBlurs().contains(targetUser.getId());
		boolean isMatched = requester.getMyMatches() != null && requester.getMyMatches().contains(targetUser.getId());
		boolean hasSentLike = requester.getMyTypes() != null && requester.getMyTypes().contains(targetUser.getId());
		boolean hasReceivedLike = requester.getMyFans() != null && requester.getMyFans().contains(targetUser.getId());
		int freeLikeRemaining = getFreeLikeRemaining(requester);

		// default: 쌍방매칭 or 쿠키 지불해서 블러 해제 → 모두 공개
		// a (NORMAL): 일반 조회 → 사진만 블러
		// b (FAN): 받은 호감 조회 → 사진 + q3 + idealType 블러
		boolean isDefault = isMatched || isPaidUnblur;
		DatingProfileDetailRequest.ViewType viewType = request != null && request.viewType() != null
			? request.viewType()
			: DatingProfileDetailRequest.ViewType.NORMAL;

		boolean photoBlurred = !isDefault;
		boolean textBlurred = !isDefault && viewType == DatingProfileDetailRequest.ViewType.FAN;

		String q3 = targetUser.getIntroduction() == null ? null : targetUser.getIntroduction().getQ3();
		String idealType = targetUser.getCard() == null ? null : targetUser.getCard().getIdealType();

		// Presigned URL 사용
		String profileUrl = photoBlurred
			? s3ObjectUrlService.getThumbnailBlurPresignedUrl(targetUser.getProfileImage())
			: s3ObjectUrlService.getThumbnailPresignedUrl(targetUser.getProfileImage());

		ContactDTO contact = isDefault
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
	public DatingMatchListResponse getMatchList(User authUser) {
		User requester = getRequiredUser(authUser);

		if (requester.getMyMatches() == null || requester.getMyMatches().isEmpty()) {
			return new DatingMatchListResponse(0, Collections.emptyList());
		}

		List<Long> matchedIds = requester.getMyMatches();

		List<DatingMatchListResponse.MatchSummary> matches = userRepository.findAllByIdIn(matchedIds).stream()
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

		// 일반 블러 해제: 2시간에 3번 무료, 초과 시 쿠키 1개
		int freeBlurRemaining = getFreeBlurRemaining(requester);
		if (freeBlurRemaining > 0) {
			requester.setFreeBlurCount(freeBlurRemaining - 1);
			if (requester.getFreeBlurResetAt() == null) {
				requester.setFreeBlurResetAt(LocalDateTime.now());
			}
		} else {
			if (requester.getCookies() == null || requester.getCookies() < 1) {
				throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
			}
			requester.setCookies(requester.getCookies() - 1);
			saveCookieUseTransaction(requester, 1, "프로필 블러 해제");
		}

		if (requester.getMyBlurs() == null) {
			requester.setMyBlurs(new java.util.ArrayList<>());
		}
		requester.getMyBlurs().add(targetUser.getId());

		return new UnblurProfileResponse(targetUser.getPublicId(), false, "프로필 블러가 해제되었습니다.");
	}

	// 받은 호감 전용 블러 해제 — 쿠키 2개
	@Transactional
	public UnblurProfileResponse unblurReceivedLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		// 실제로 받은 호감인지 확인
		if (requester.getMyFans() == null || !requester.getMyFans().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}

		if (requester.getMyBlurs() != null && requester.getMyBlurs().contains(targetUser.getId())) {
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

		return new UnblurProfileResponse(targetUser.getPublicId(), false, "소개팅 카드 블러가 해제되었습니다.");
	}

	@Transactional
	public void matchReceivedLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

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
	}

	@Transactional
	public void sendLike(User authUser, String publicId) {
		User requester = getRequiredUser(authUser);
		User targetUser = userRepository.findByPublicId(publicId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		if (requester.getId().equals(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.CANNOT_LIKE_SELF);
		}

		if (requester.getMyTypes() != null && requester.getMyTypes().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.ALREADY_LIKED);
		}
		if (requester.getMyMatches() != null && requester.getMyMatches().contains(targetUser.getId())) {
			throw new GlobalException(GlobalErrorCode.ALREADY_LIKED);
		}

		// 2시간에 3번 무료, 초과 시 쿠키 1개
		int freeLikeRemaining = getFreeLikeRemaining(requester);
		if (freeLikeRemaining > 0) {
			requester.setFreeLikeCount(requester.getFreeLikeCount() + 1);
			if (requester.getFreeLikeResetAt() == null) {
				requester.setFreeLikeResetAt(LocalDateTime.now());
			}
		} else {
			if (requester.getCookies() == null || requester.getCookies() < 1) {
				throw new GlobalException(GlobalErrorCode.INSUFFICIENT_COOKIES);
			}
			requester.setCookies(requester.getCookies() - 1);
			saveCookieUseTransaction(requester, 1, "호감 보내기");
		}

		if (requester.getMyTypes() == null) {
			requester.setMyTypes(new java.util.ArrayList<>());
		}
		requester.getMyTypes().add(targetUser.getId());

		if (targetUser.getMyFans() == null) {
			targetUser.setMyFans(new java.util.ArrayList<>());
		}
		targetUser.getMyFans().add(requester.getId());

		// 쌍방 매칭 확인 — 상대도 나한테 호감 보낸 상태면 match 목록으로 이동한다.
		boolean isMutual = targetUser.getMyTypes() != null && targetUser.getMyTypes().contains(requester.getId());
		if (isMutual) {
			addMatch(requester, targetUser);
		}
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
	}

	@Transactional
	public int refreshReadyNowShows() {
		LocalDateTime now = LocalDateTime.now();
		List<User> users = userRepository.findUsersReadyForNowShowRefresh(now);
		users.forEach(user -> refreshNowShows(user, now));
		return users.size();
	}

	private void saveCookieUseTransaction(User user, int amount, String description) {
		cookieTransactionRepository.save(CookieTransaction.use(user, amount, description));
	}

	private void refreshNowShows(User user, LocalDateTime now) {
		user.setNowShows(new ArrayList<>(findRandomOppositeGenderUserIds(user)));
		user.setRefreshAvailableAt(nextRefreshAvailableAt(now));
		user.setFreeBlurCount(3);
		user.setFreeBlurResetAt(now);
	}

	private void addMatch(User user, User target) {
		addUniqueRelation(user, target.getId(), RelationType.MY_MATCHES);
		addUniqueRelation(target, user.getId(), RelationType.MY_MATCHES);
		addUniqueRelation(user, target.getId(), RelationType.MY_BLURS);
		addUniqueRelation(target, user.getId(), RelationType.MY_BLURS);
		removeRelation(user, target.getId(), RelationType.MY_FANS);
		removeRelation(user, target.getId(), RelationType.MY_TYPES);
		removeRelation(user, target.getId(), RelationType.NOW_SHOWS);
		removeRelation(target, user.getId(), RelationType.MY_FANS);
		removeRelation(target, user.getId(), RelationType.MY_TYPES);
		removeRelation(target, user.getId(), RelationType.NOW_SHOWS);
	}

	private void addUniqueRelation(User user, Long targetUserId, RelationType relationType) {
		List<Long> values = getOrCreateRelationList(user, relationType);
		if (!values.contains(targetUserId)) {
			values.add(targetUserId);
		}
	}

	private void removeRelation(User user, Long targetUserId, RelationType relationType) {
		List<Long> values = getRelationList(user, relationType);
		if (values != null) {
			values.remove(targetUserId);
		}
	}

	private List<Long> getOrCreateRelationList(User user, RelationType relationType) {
		return switch (relationType) {
			case MY_TYPES -> {
				if (user.getMyTypes() == null) {
					user.setMyTypes(new ArrayList<>());
				}
				yield user.getMyTypes();
			}
			case MY_MATCHES -> {
				if (user.getMyMatches() == null) {
					user.setMyMatches(new ArrayList<>());
				}
				yield user.getMyMatches();
			}
			case MY_FANS -> {
				if (user.getMyFans() == null) {
					user.setMyFans(new ArrayList<>());
				}
				yield user.getMyFans();
			}
			case MY_BLURS -> {
				if (user.getMyBlurs() == null) {
					user.setMyBlurs(new ArrayList<>());
				}
				yield user.getMyBlurs();
			}
			case NOW_SHOWS -> {
				if (user.getNowShows() == null) {
					user.setNowShows(new ArrayList<>());
				}
				yield user.getNowShows();
			}
		};
	}

	private List<Long> getRelationList(User user, RelationType relationType) {
		return switch (relationType) {
			case MY_TYPES -> user.getMyTypes();
			case MY_MATCHES -> user.getMyMatches();
			case MY_FANS -> user.getMyFans();
			case MY_BLURS -> user.getMyBlurs();
			case NOW_SHOWS -> user.getNowShows();
		};
	}

	private boolean canHaveNowShows(User user) {
		return Boolean.TRUE.equals(user.getIsRegistered())
			&& user.hasIntroduction()
			&& user.hasCard();
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
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime resetAt = user.getFreeLikeResetAt();

		if (resetAt == null || resetAt.plusHours(2).isBefore(now)) {
			user.setFreeLikeCount(0);
			user.setFreeLikeResetAt(now);
		}
		return Math.max(0, 3 - user.getFreeLikeCount());
	}

	private int getFreeBlurRemaining(User user) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime resetAt = user.getFreeBlurResetAt();

		if (user.getFreeBlurCount() == null || resetAt == null || resetAt.plusHours(2).isBefore(now)) {
			user.setFreeBlurCount(3);
			user.setFreeBlurResetAt(now);
		}
		return Math.max(0, user.getFreeBlurCount());
	}

	private enum RelationType {
		MY_TYPES,
		MY_MATCHES,
		MY_FANS,
		MY_BLURS,
		NOW_SHOWS
	}
}
