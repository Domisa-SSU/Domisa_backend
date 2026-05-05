package com.domisa.domisa_backend.dummy.service;

import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.jwt.JwtProvider;
import com.domisa.domisa_backend.auth.service.AuthCookieManager;
import com.domisa.domisa_backend.dummy.dto.DummyLoginRequest;
import com.domisa.domisa_backend.dummy.dto.DummyLoginResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserCreateRequest;
import com.domisa.domisa_backend.dummy.dto.DummyUserCreateResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserListResponse;
import com.domisa.domisa_backend.dummy.dto.DummyUserResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DummyDataService {

	private static final int MAX_DUMMY_USER_COUNT = 50;
	private static final long DUMMY_KAKAO_ID_START = 9_000_000_001L;
	private static final long DUMMY_KAKAO_ID_END = DUMMY_KAKAO_ID_START + MAX_DUMMY_USER_COUNT - 1;

	private final UserRepository userRepository;
	private final JwtProvider jwtProvider;
	private final AuthCookieManager authCookieManager;

	@Value("${app.dummy.admin-key:}")
	private String dummyAdminKey;

	@Transactional
	public DummyUserCreateResponse createDummyUsers(String adminKey, DummyUserCreateRequest request) {
		validateAdminKey(adminKey);

		User user = userRepository.save(createDummyUser(resolveNextDummyKakaoId()));
		userRepository.flush();
		return new DummyUserCreateResponse(toResponse(user));
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

	private User createDummyUser(long kakaoId) {
		User user = User.create(kakaoId);
		user.setName("Dummy User " + kakaoId);
		user.setCookies(20L);
		user.setMyFans(new ArrayList<>());
		user.setMyTypes(new ArrayList<>());
		user.setMyBlurs(new ArrayList<>());
		user.setNowShows(new ArrayList<>());
		return user;
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

	private long resolveNextDummyKakaoId() {
		return findDummyUsers().stream()
			.map(User::getKakaoId)
			.max(Long::compareTo)
			.map(kakaoId -> {
				long nextKakaoId = kakaoId + 1;
				if (nextKakaoId > DUMMY_KAKAO_ID_END) {
					throw new GlobalException(GlobalErrorCode.INVALID_DUMMY_USER_COUNT);
				}
				return nextKakaoId;
			})
			.orElse(DUMMY_KAKAO_ID_START);
	}

	private DummyUserResponse toResponse(User user) {
		return DummyUserResponse.from(user, Map.of(user.getId(), user.getPublicId()));
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
}
