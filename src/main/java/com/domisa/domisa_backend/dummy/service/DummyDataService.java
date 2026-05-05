package com.domisa.domisa_backend.dummy.service;

import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.jwt.JwtProvider;
import com.domisa.domisa_backend.auth.service.AuthCookieManager;
import com.domisa.domisa_backend.card.entity.Card;
import com.domisa.domisa_backend.card.repository.CardRepository;
import com.domisa.domisa_backend.dummy.dto.DummyLoginRequest;
import com.domisa.domisa_backend.dummy.dto.DummyLoginResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserCreateRequest;
import com.domisa.domisa_backend.dummy.dto.DummyUserCreateResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserListResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.type.Mbti;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DummyDataService {

	private static final int DEFAULT_DUMMY_USER_COUNT = 12;
	private static final int MAX_DUMMY_USER_COUNT = 50;
	private static final int MAX_NOW_SHOW_COUNT = 8;
	private static final long DUMMY_KAKAO_ID_START = 9_000_000_001L;
	private static final long DUMMY_KAKAO_ID_END = DUMMY_KAKAO_ID_START + MAX_DUMMY_USER_COUNT - 1;

	private final UserRepository userRepository;
	private final CardRepository cardRepository;
	private final IntroductionRepository introductionRepository;
	private final ProfileImageRepository profileImageRepository;
	private final JwtProvider jwtProvider;
	private final AuthCookieManager authCookieManager;

	@Value("${app.dummy.admin-key:}")
	private String dummyAdminKey;

	@Transactional
	public DummyUserCreateResponse createDummyUsers(String adminKey, DummyUserCreateRequest request) {
		validateAdminKey(adminKey);

		int requestedCount = resolveRequestedCount(request);
		List<User> existingDummyUsers = findDummyUsers();
		Map<Long, User> existingUsersByKakaoId = existingDummyUsers.stream()
			.collect(Collectors.toMap(User::getKakaoId, Function.identity()));

		List<User> createdUsers = new ArrayList<>();
		for (int index = 1; index <= requestedCount; index++) {
			long kakaoId = DUMMY_KAKAO_ID_START + index - 1;
			if (existingUsersByKakaoId.containsKey(kakaoId)) {
				continue;
			}
			User user = createDummyUser(index, kakaoId);
			createdUsers.add(userRepository.save(user));
		}

		userRepository.flush();

		List<User> dummyUsers = findDummyUsers().stream()
			.limit(requestedCount)
			.toList();
		createProfileState(dummyUsers);
		createRelationState(dummyUsers);

		return new DummyUserCreateResponse(
			requestedCount,
			createdUsers.size(),
			dummyUsers.size(),
			toResponses(dummyUsers)
		);
	}

	@Transactional(readOnly = true)
	public DummyUserListResponse getDummyUsers(String adminKey) {
		validateAdminKey(adminKey);

		List<User> users = findDummyUsers();
		return new DummyUserListResponse(users.size(), toResponses(users));
	}

	@Transactional
	public DummyLoginResponse login(
		String adminKey,
		DummyLoginRequest request,
		HttpServletResponse response
	) {
		validateAdminKey(adminKey);
		if (request == null || (!StringUtils.hasText(request.publicId()) && request.kakaoId() == null)) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}

		User user = findLoginUser(request);
		String accessToken = jwtProvider.createAccessToken(user.getId());
		String refreshToken = jwtProvider.createRefreshToken(user.getId());

		authCookieManager.addCookie(
			response,
			"accessToken",
			accessToken,
			Duration.ofMillis(jwtProvider.getAccessTokenValidityMs())
		);
		authCookieManager.addCookie(
			response,
			"refreshToken",
			refreshToken,
			Duration.ofMillis(jwtProvider.getRefreshTokenValidityMs())
		);

		return new DummyLoginResponse(
			user.getPublicId(),
			user.getKakaoId(),
			user.getNickname(),
			new LoginResponse.StatusDto(
				user.getIsRegistered(),
				user.hasIntroduction(),
				user.hasCard()
			),
			List.of("accessToken", "refreshToken")
		);
	}

	private User createDummyUser(int index, long kakaoId) {
		User user = User.create(kakaoId);
		user.setName("Dummy User " + index);
		user.setNickname("D" + String.format("%03d", index));
		user.setGender(index % 2 == 1);
		user.setBirthYear(1994L + (index % 10));
		user.setAnimalProfile(AnimalProfile.values()[(index - 1) % AnimalProfile.values().length]);
		user.setCookies(20L);
		user.setContactType(index % 2 == 0 ? ContactType.INSTAGRAM : ContactType.KAKAO);
		user.setContact("dummy_contact_" + index);
		user.setInviteCode("DUMMY" + String.format("%03d", index));
		user.setNotificationPhone("0100000" + String.format("%04d", index));
		user.setIsRegistered(true);
		user.setIsProfileCompleted(true);
		user.setHasIntroduction(true);
		user.setRefreshAt(LocalDateTime.now());
		user.setFreeBlurCount(index % 3);
		user.setFreeBlurResetAt(LocalDateTime.now().minusMinutes(20));
		user.setFreeLikeCount(index % 3);
		user.setFreeLikeResetAt(LocalDateTime.now().minusMinutes(20));
		user.setMyFans(new ArrayList<>());
		user.setMyTypes(new ArrayList<>());
		user.setMyBlurs(new ArrayList<>());
		user.setNowShows(new ArrayList<>());
		return user;
	}

	private void createProfileState(List<User> users) {
		for (int index = 0; index < users.size(); index++) {
			User user = users.get(index);
			int displayIndex = index + 1;

			cardRepository.findByUserId(user.getId())
				.orElseGet(() -> {
					Card card = Card.create(
						user,
						Mbti.values()[(displayIndex - 1) % Mbti.values().length],
						"Prefers steady conversations and clear plans.",
						"Someone curious, kind, and easy to talk with.",
						"dummy/cards/card-" + displayNumber(displayIndex) + ".jpg"
					);
					user.setCard(card);
					return cardRepository.save(card);
				});

			profileImageRepository.findByUserId(user.getId())
				.orElseGet(() -> {
					ProfileImage profileImage = ProfileImage.create(user);
					String prefix = "dummy/profile-images/user-" + displayNumber(displayIndex);
					profileImage.prepareUpload(
						1L,
						prefix + "/origin.jpg",
						prefix + "/origin-blur.jpg",
						prefix + "/thumbnail.jpg",
						prefix + "/thumbnail-blur.jpg"
					);
					profileImage.markReady();
					return profileImageRepository.save(profileImage);
				});

			if (user.getIntroduction() == null) {
				User introducer = users.size() > 1 ? users.get((index + 1) % users.size()) : null;
				Introduction introduction = Introduction.create(
					"Dummy answer about personality " + displayIndex,
					"Dummy answer about dating style " + displayIndex,
					"Dummy answer about a memorable detail " + displayIndex,
					introducer,
					"DUMMY" + displayNumber(displayIndex)
				);
				introduction.assignParticipant(user);
				introductionRepository.save(introduction);
			}

			user.setIsRegistered(true);
			user.setIsProfileCompleted(true);
			user.setHasIntroduction(true);
		}
	}

	private void createRelationState(List<User> users) {
		int size = users.size();
		if (size < 2) {
			return;
		}

		for (int index = 0; index < size; index++) {
			User user = users.get(index);
			ensureLists(user);

			for (int offset = 1; offset <= Math.min(MAX_NOW_SHOW_COUNT, size - 1); offset++) {
				User target = users.get((index + offset) % size);
				addUnique(user.getNowShows(), target.getId());
			}

			addLike(user, users.get((index + 1) % size));
			addLike(user, users.get((index + 2) % size));

			if (index % 2 == 0) {
				User mutualTarget = users.get((index + 1) % size);
				addLike(mutualTarget, user);
				addUnique(user.getMyBlurs(), mutualTarget.getId());
				addUnique(mutualTarget.getMyBlurs(), user.getId());
			}

			User unblurTarget = users.get((index + 3) % size);
			if (!user.getId().equals(unblurTarget.getId())) {
				addUnique(user.getMyBlurs(), unblurTarget.getId());
			}
		}
	}

	private void addLike(User requester, User target) {
		if (requester.getId().equals(target.getId())) {
			return;
		}
		ensureLists(requester);
		ensureLists(target);
		addUnique(requester.getMyTypes(), target.getId());
		addUnique(target.getMyFans(), requester.getId());
	}

	private void ensureLists(User user) {
		if (user.getNowShows() == null) {
			user.setNowShows(new ArrayList<>());
		}
		if (user.getMyFans() == null) {
			user.setMyFans(new ArrayList<>());
		}
		if (user.getMyTypes() == null) {
			user.setMyTypes(new ArrayList<>());
		}
		if (user.getMyBlurs() == null) {
			user.setMyBlurs(new ArrayList<>());
		}
	}

	private void addUnique(List<Long> values, Long value) {
		if (value != null && !values.contains(value)) {
			values.add(value);
		}
	}

	private List<User> findDummyUsers() {
		return userRepository.findByKakaoIdBetweenOrderByKakaoIdAsc(DUMMY_KAKAO_ID_START, DUMMY_KAKAO_ID_END);
	}

	private User findLoginUser(DummyLoginRequest request) {
		if (StringUtils.hasText(request.publicId())) {
			User user = userRepository.findByPublicId(request.publicId())
				.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
			validateDummyUser(user);
			return user;
		}

		User user = userRepository.findByKakaoId(request.kakaoId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
		validateDummyUser(user);
		return user;
	}

	private void validateDummyUser(User user) {
		Long kakaoId = user.getKakaoId();
		if (kakaoId == null || kakaoId < DUMMY_KAKAO_ID_START || kakaoId > DUMMY_KAKAO_ID_END) {
			throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
		}
	}

	private int resolveRequestedCount(DummyUserCreateRequest request) {
		int count = request == null || request.count() == null ? DEFAULT_DUMMY_USER_COUNT : request.count();
		if (count < 1 || count > MAX_DUMMY_USER_COUNT) {
			throw new GlobalException(GlobalErrorCode.INVALID_DUMMY_USER_COUNT);
		}
		return count;
	}

	private List<DummyUserResponse> toResponses(List<User> users) {
		Map<Long, String> publicIdsByUserId = resolvePublicIds(users);
		return users.stream()
			.map(user -> DummyUserResponse.from(user, publicIdsByUserId))
			.toList();
	}

	private Map<Long, String> resolvePublicIds(List<User> users) {
		Set<Long> relationUserIds = users.stream()
			.map(this::collectRelationUserIds)
			.flatMap(Collection::stream)
			.collect(Collectors.toCollection(LinkedHashSet::new));

		Map<Long, String> publicIdsByUserId = new LinkedHashMap<>();
		users.forEach(user -> publicIdsByUserId.put(user.getId(), user.getPublicId()));
		if (!relationUserIds.isEmpty()) {
			userRepository.findAllById(relationUserIds)
				.forEach(user -> publicIdsByUserId.put(user.getId(), user.getPublicId()));
		}
		return publicIdsByUserId;
	}

	private Set<Long> collectRelationUserIds(User user) {
		Set<Long> userIds = new LinkedHashSet<>();
		addAll(userIds, user.getNowShows());
		addAll(userIds, user.getMyFans());
		addAll(userIds, user.getMyTypes());
		addAll(userIds, user.getMyBlurs());
		return userIds;
	}

	private void addAll(Set<Long> target, List<Long> source) {
		if (source != null) {
			target.addAll(source);
		}
	}

	private void validateAdminKey(String adminKey) {
		if (!StringUtils.hasText(dummyAdminKey)) {
			throw new GlobalException(GlobalErrorCode.DUMMY_ADMIN_KEY_NOT_CONFIGURED);
		}
		if (!dummyAdminKey.equals(adminKey)) {
			throw new GlobalException(GlobalErrorCode.DUMMY_ADMIN_UNAUTHORIZED);
		}
	}

	private String displayNumber(int index) {
		return String.format("%03d", index);
	}
}
