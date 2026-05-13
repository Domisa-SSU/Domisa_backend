package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.auth.blacklist.UserBlacklistService;
import com.domisa.domisa_backend.auth.blacklist.entity.UserBlacklist;
import com.domisa.domisa_backend.auth.blacklist.repository.UserBlacklistRepository;
import com.domisa.domisa_backend.dms.dto.DmsUserDetailResponse;
import com.domisa.domisa_backend.dms.dto.DmsUserListResponse;
import com.domisa.domisa_backend.dms.dto.DmsUserStatsResponse;
import com.domisa.domisa_backend.dating.service.DatingService;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.card.entity.Card;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmsUserService {

	private static final int DMS_USER_PAGE_SIZE = 20;
	private static final int DMS_PAGE_GROUP_SIZE = 10;

	private final UserRepository userRepository;
	private final UserBlacklistRepository userBlacklistRepository;
	private final UserBlacklistService userBlacklistService;
	private final CookieTransactionRepository cookieTransactionRepository;
	private final ProfileImageRepository profileImageRepository;
	private final S3ObjectUrlService s3ObjectUrlService;
	private final UserService userService;
	private final DatingService datingService;

	@Transactional(readOnly = true)
	public DmsUserListResponse getUsers(String checked, String status, String keyword, Integer page, boolean completedOnly) {
		int requestedPage = normalizeRequestedPage(page);
		String normalizedKeyword = normalizeKeyword(keyword);
		Page<User> userPage = userRepository.findAllForDms(
			checked,
			status,
			normalizedKeyword,
			completedOnly,
			PageRequest.of(requestedPage - 1, DMS_USER_PAGE_SIZE)
		);
		if (requestedPage > 1 && userPage.getTotalPages() > 0 && requestedPage > userPage.getTotalPages()) {
			userPage = userRepository.findAllForDms(
				checked,
				status,
				normalizedKeyword,
				completedOnly,
				PageRequest.of(userPage.getTotalPages() - 1, DMS_USER_PAGE_SIZE)
			);
		}

		List<Long> userIds = userPage.getContent().stream().map(User::getId).toList();
		Set<Long> blacklistedUserIds = findBlacklistedUserIds(userIds);
		Set<Long> profileImageUserIds = findProfileImageUserIds(userIds);
		List<DmsUserListResponse.UserRow> rows = userPage.getContent().stream()
			.map(user -> toUserRow(
				user,
				blacklistedUserIds.contains(user.getId()),
				profileImageUserIds.contains(user.getId())
			))
			.toList();
		int currentPage = userPage.getNumber() + 1;
		int totalPages = Math.max(1, userPage.getTotalPages());
		int currentGroupStart = ((currentPage - 1) / DMS_PAGE_GROUP_SIZE) * DMS_PAGE_GROUP_SIZE + 1;
		int currentGroupEnd = Math.min(totalPages, currentGroupStart + DMS_PAGE_GROUP_SIZE - 1);
		boolean hasPreviousGroup = currentGroupStart > 1;
		boolean hasNextGroup = currentGroupEnd < totalPages;
		int previousGroupPage = Math.max(1, currentGroupStart - DMS_PAGE_GROUP_SIZE);
		int nextGroupPage = Math.min(totalPages, currentGroupStart + DMS_PAGE_GROUP_SIZE);

		return new DmsUserListResponse(
			getStats(completedOnly),
			rows,
			checked,
			status,
			normalizedKeyword,
			completedOnly,
			currentPage,
			DMS_USER_PAGE_SIZE,
			totalPages,
			userPage.getTotalElements(),
			currentPage > 1,
			currentPage < totalPages,
			hasPreviousGroup,
			hasNextGroup,
			previousGroupPage,
			nextGroupPage,
			buildPageNumbers(currentGroupStart, currentGroupEnd)
		);
	}

	@Transactional(readOnly = true)
	public DmsUserDetailResponse getUserDetail(Long userId) {
		User user = userRepository.findDmsDetailById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
		return toUserDetail(user, userBlacklistRepository.existsByUserId(userId));
	}

	@Transactional
	public void markHeaven(Long userId) {
		User user = getUser(userId);
		user.markChecked();
		userBlacklistService.removeBlacklist(userId);
	}

	@Transactional
	public void markHell(Long userId) {
		User user = getUser(userId);
		user.markChecked();
		userBlacklistService.blacklist(userId);
	}

	@Transactional
	public void addCookies(Long userId, long amount, String reason) {
		validatePositiveAmount(amount);
		User user = getUser(userId);
		user.addCookies(amount);
		cookieTransactionRepository.save(CookieTransaction.reward(user, Math.toIntExact(amount), buildReason("관리자 쿠키 지급", reason)));
	}

	@Transactional
	public void subtractCookies(Long userId, long amount, String reason) {
		validatePositiveAmount(amount);
		User user = getUser(userId);
		if (user.getCookieBalance() < amount) {
			throw new IllegalArgumentException("보유 쿠키보다 많이 차감할 수 없습니다.");
		}
		user.subtractCookies(amount);
		cookieTransactionRepository.save(CookieTransaction.use(user, Math.toIntExact(amount), buildReason("관리자 쿠키 차감", reason)));
	}

	@Transactional
	public int addCookiesToAll(long amount) {
		validatePositiveAmount(amount);
		return userRepository.addCookiesToAll(amount);
	}

	public void deleteUser(Long userId) {
		userService.deleteUserById(userId);
	}

	public void refreshNowShows(Long userId) {
		datingService.refreshNowShowsByAdmin(userId);
	}

	@Transactional(readOnly = true)
	public String getStudentCardPresignedUrl(Long userId) {
		User user = getUser(userId);
		String studentCardKey = user.getStudentCardKey();
		if (studentCardKey == null || studentCardKey.isBlank()) {
			throw new IllegalArgumentException("학생증 사진이 없습니다.");
		}
		return s3ObjectUrlService.buildPresignedGetUrl(studentCardKey);
	}

	@Transactional(readOnly = true)
	public String getProfileImagePresignedUrl(Long userId) {
		ProfileImage profileImage = profileImageRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("프로필 사진이 없습니다."));
		String url = s3ObjectUrlService.getProfileImagePresignedUrl(profileImage);
		if (url == null) {
			throw new IllegalArgumentException("프로필 사진이 없습니다.");
		}
		return url;
	}

	private DmsUserStatsResponse getStats(boolean completedOnly) {
		LocalDate today = LocalDate.now();
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = today.plusDays(1).atStartOfDay();
		return new DmsUserStatsResponse(
			userRepository.countForDms(completedOnly),
			userRepository.countDmsCheckedUsers(completedOnly),
			userRepository.countDmsUncheckedUsers(completedOnly),
			userRepository.countByGenderForDms(true, completedOnly),
			userRepository.countByGenderForDms(false, completedOnly),
			userRepository.countByCreatedAtBetweenForDms(start, end, completedOnly)
		);
	}

	private Set<Long> findBlacklistedUserIds(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		return userBlacklistRepository.findByUserIdIn(userIds).stream()
			.map(UserBlacklist::getUser)
			.map(User::getId)
			.collect(java.util.stream.Collectors.toCollection(HashSet::new));
	}

	private Set<Long> findProfileImageUserIds(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(profileImageRepository.findAvailableProfileImageUserIds(
			userIds,
			List.of(ProfileImageProcessingStatus.READY, ProfileImageProcessingStatus.COMPLETED)
		));
	}

	private int normalizeRequestedPage(Integer page) {
		if (page == null || page < 1) {
			return 1;
		}
		return page;
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null) {
			return null;
		}
		String normalized = keyword.strip();
		return normalized.isEmpty() ? null : normalized;
	}

	private List<Integer> buildPageNumbers(int start, int end) {
		List<Integer> pageNumbers = new ArrayList<>();
		for (int page = start; page <= end; page++) {
			pageNumbers.add(page);
		}
		return pageNumbers;
	}

	private DmsUserListResponse.UserRow toUserRow(User user, boolean blacklisted, boolean hasProfileImage) {
		return new DmsUserListResponse.UserRow(
			user.getId(),
			user.getPublicId(),
			user.getNickname(),
			user.getGenderDisplay(),
			user.getBirthYear(),
			user.getCookieBalance(),
			user.getIsRegistered(),
			user.getHasIntroduction(),
			user.getIsProfileCompleted(),
			user.getIsChecked(),
			blacklisted,
			hasProfileImage,
			user.getCreatedAt()
		);
	}

	private DmsUserDetailResponse toUserDetail(User user, boolean blacklisted) {
		return new DmsUserDetailResponse(
			user.getId(),
			user.getPublicId(),
			user.getKakaoId(),
			user.getName(),
			user.getNickname(),
			user.getGenderDisplay(),
			user.getBirthYear(),
			user.getAge(),
			user.getAnimalProfile() == null ? null : user.getAnimalProfile().name(),
			user.getCookieBalance(),
			user.getContactType() == null ? null : user.getContactType().name(),
			user.getContact(),
			user.getInviteCode(),
			user.getIsRegistered(),
			user.getHasIntroduction(),
			user.getIsProfileCompleted(),
			user.getNotificationPhone(),
			user.getFreeLikeCount(),
			user.getStudentCardKey(),
			user.getIsChecked(),
			blacklisted,
			user.getRefreshAvailableAt(),
			user.getCreatedAt(),
			user.getUpdatedAt(),
			copyList(user.getMyBlurs()),
			copyList(user.getMyFans()),
				copyList(user.getMyTypes()),
				copyList(user.getMyMatches()),
				copyList(user.getNowShows()),
				buildDmsProfileImageUrl(user),
				toIntroductionDetail(user.getIntroduction()),
				toCardDetail(user.getCard())
			);
	}

	private DmsUserDetailResponse.IntroductionDetail toIntroductionDetail(Introduction introduction) {
		if (introduction == null) {
			return null;
		}
		User introducer = introduction.getIntroducer();
		return new DmsUserDetailResponse.IntroductionDetail(
			introduction.getId(),
			introducer == null ? null : introducer.getId(),
			introducer == null ? null : introducer.getPublicId(),
			introducer == null ? null : introducer.getNickname(),
			introduction.getLinkCode(),
			introduction.getQ1(),
			introduction.getQ2(),
			introduction.getQ3()
		);
	}

	private DmsUserDetailResponse.CardDetail toCardDetail(Card card) {
		if (card == null) {
			return null;
		}
		return new DmsUserDetailResponse.CardDetail(
			card.getId(),
			card.getMbti() == null ? null : card.getMbti().name(),
			card.getDatingStyle(),
			card.getIdealType()
		);
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
	}

	private void validatePositiveAmount(long amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("쿠키 수량은 1 이상이어야 합니다.");
		}
		Math.toIntExact(amount);
	}

	private String buildReason(String defaultReason, String reason) {
		if (reason == null || reason.isBlank()) {
			return defaultReason;
		}
		String description = defaultReason + ": " + reason.strip();
		return description.length() > 100 ? description.substring(0, 100) : description;
	}

	private String buildDmsProfileImageUrl(User user) {
		ProfileImage profileImage = user.getProfileImage();
		if (profileImage == null
			|| profileImage.getProfileOriginKey() == null
			|| profileImage.getProfileOriginKey().isBlank()
			|| (profileImage.getProcessingStatus() != ProfileImageProcessingStatus.READY
			&& profileImage.getProcessingStatus() != ProfileImageProcessingStatus.COMPLETED)) {
			return null;
		}
		return "/dms-room/users/" + user.getId() + "/profile-image";
	}

	private List<Long> copyList(List<Long> values) {
		return values == null ? List.of() : new ArrayList<>(values);
	}
}
