package com.domisa.domisa_backend.profile.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
import com.domisa.domisa_backend.profile.dto.ProfileUpdateRequest;
import com.domisa.domisa_backend.profile.dto.ProfileUpdateResponse;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final UserRepository userRepository;

	// 닉네임 중복 조회
	@Transactional(readOnly = true)
	public Map<String, Boolean> checkNickname(String nickname) {
		validateNickname(nickname);
		return Map.of("isAvailable", !userRepository.existsByNickname(nickname));
	}

	// 회원가입
	@Transactional
	public ProfileRegisterResponse registerProfile(Long userId, ProfileRegisterRequest request) {
		validateRequiredFields(request);
		validateNickname(request.nickname());

		// 닉네임 중복 체크
		if (userRepository.existsByNickname(request.nickname())) {
			throw new GlobalException(GlobalErrorCode.DUPLICATE_NICKNAME);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		user.setNickname(request.nickname());
		user.setGender(request.gender());
		user.setBirthYear(request.birthYear());
		user.setAnimalProfile(request.animalProfile());
		user.setIsRegistered(true);
		long totalUserCount = Math.max(0, userRepository.count() - 1);

		return new ProfileRegisterResponse(
				user.getId(),
				new ProfileRegisterResponse.StatusDto(
						user.getIsRegistered(),
						user.hasIntroduction(),
						user.hasCard()
				),
				totalUserCount
		);
	}

	// 프로필 수정
	@Transactional
	public ProfileUpdateResponse updateProfile(Long userId, ProfileUpdateRequest request) {
		validateNickname(request.nickname());

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		// 내 닉네임이랑 다를때만 중복 체크 (같은 닉네임 유지는 허용)
		if(!user.getNickname().equals(request.nickname())
		&& userRepository.existsByNickname(request.nickname())) {
			throw new GlobalException(GlobalErrorCode.DUPLICATE_NICKNAME);
		}
		user.setNickname(request.nickname());
		user.setGender(request.gender());
		user.setBirthYear(request.birthYear());
		user.setAnimalProfile(request.animalProfile());

		return new ProfileUpdateResponse(
				user.getId(),
				user.getNickname(),
				user.getGender(),
				user.getBirthYear(),
				user.getAnimalProfile()
		);
	}

	private void validateRequiredFields(ProfileRegisterRequest request) {
		if (request.nickname() == null || request.nickname().isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
	}

	private void validateNickname(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
		}
		if (nickname.length() > 4) {
			throw new GlobalException(GlobalErrorCode.INVALID_NICKNAME_LENGTH);
		}
	}
}
