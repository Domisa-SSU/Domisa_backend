package com.domisa.domisa_backend.admin.service;

import com.domisa.domisa_backend.auth.blacklist.UserBlacklistService;
import com.domisa.domisa_backend.auth.blacklist.entity.UserBlacklist;
import com.domisa.domisa_backend.auth.blacklist.repository.UserBlacklistRepository;
import com.domisa.domisa_backend.admin.dto.DmsUserDetailResponse;
import com.domisa.domisa_backend.admin.dto.DmsUserListResponse;
import com.domisa.domisa_backend.admin.dto.DmsUserStatsResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
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

	private final UserRepository userRepository;
	private final UserBlacklistRepository userBlacklistRepository;
	private final UserBlacklistService userBlacklistService;
	private final CookieTransactionRepository cookieTransactionRepository;
	private final S3ObjectUrlService s3ObjectUrlService;

	@Transactional(readOnly = true)
	public DmsUserListResponse getUsers(String checked, String status, Integer page) {
		int requestedPage = normalizeRequestedPage(page);
		Page<User> userPage = userRepository.findAllForDms(
			checked,
			status,
			PageRequest.of(requestedPage - 1, DMS_USER_PAGE_SIZE)
		);
		if (requestedPage > 1 && userPage.getTotalPages() > 0 && requestedPage > userPage.getTotalPages()) {
			userPage = userRepository.findAllForDms(
				checked,
				status,
				PageRequest.of(userPage.getTotalPages() - 1, DMS_USER_PAGE_SIZE)
			);
		}

		Set<Long> blacklistedUserIds = findBlacklistedUserIds(userPage.getContent().stream().map(User::getId).toList());
		List<DmsUserListResponse.UserRow> rows = userPage.getContent().stream()
			.map(user -> toUserRow(user, blacklistedUserIds.contains(user.getId())))
			.toList();
		int currentPage = userPage.getNumber() + 1;
		int totalPages = Math.max(1, userPage.getTotalPages());

		return new DmsUserListResponse(
			getStats(),
			rows,
			checked,
			status,
			currentPage,
			DMS_USER_PAGE_SIZE,
			totalPages,
			userPage.getTotalElements(),
			currentPage > 1,
			currentPage < totalPages,
			buildPageNumbers(currentPage, totalPages)
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

	@Transactional(readOnly = true)
	public String getStudentCardPresignedUrl(Long userId) {
		User user = getUser(userId);
		String studentCardKey = user.getStudentCardKey();
		if (studentCardKey == null || studentCardKey.isBlank()) {
			throw new IllegalArgumentException("학생증 사진이 없습니다.");
		}
		return s3ObjectUrlService.buildPresignedGetUrl(studentCardKey);
	}

	private DmsUserStatsResponse getStats() {
		LocalDate today = LocalDate.now();
		LocalDateTime start = today.atStartOfDay();
		LocalDateTime end = today.plusDays(1).atStartOfDay();
		return new DmsUserStatsResponse(
			userRepository.count(),
			userRepository.countDmsCheckedUsers(),
			userRepository.countDmsUncheckedUsers(),
			userRepository.countByGender(true),
			userRepository.countByGender(false),
			userRepository.countByCreatedAtBetween(start, end)
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

	private int normalizeRequestedPage(Integer page) {
		if (page == null || page < 1) {
			return 1;
		}
		return page;
	}

	private List<Integer> buildPageNumbers(int currentPage, int totalPages) {
		int start = Math.max(1, currentPage - 2);
		int end = Math.min(totalPages, currentPage + 2);
		List<Integer> pageNumbers = new ArrayList<>();
		for (int page = start; page <= end; page++) {
			pageNumbers.add(page);
		}
		return pageNumbers;
	}

	private DmsUserListResponse.UserRow toUserRow(User user, boolean blacklisted) {
		return new DmsUserListResponse.UserRow(
			user.getId(),
			user.getPublicId(),
			user.getName(),
			user.getNickname(),
			user.getGenderDisplay(),
			user.getBirthYear(),
			user.getCookieBalance(),
			user.getIsChecked(),
			blacklisted,
			user.getStudentCardKey(),
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
			buildProfileImageUrl(user.getProfileImage())
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

	private String buildProfileImageUrl(ProfileImage profileImage) {
		if (profileImage == null) {
			return null;
		}
		String objectKey = profileImage.getProfileOriginKey();
		if (objectKey == null || objectKey.isBlank()) {
			objectKey = profileImage.getProfileThumbnailKey();
		}
		return buildPresignedUrl(objectKey);
	}

	private String buildPresignedUrl(String objectKey) {
		return objectKey == null || objectKey.isBlank() ? null : s3ObjectUrlService.buildPresignedGetUrl(objectKey);
	}

	private List<Long> copyList(List<Long> values) {
		return values == null ? List.of() : new ArrayList<>(values);
	}
}
