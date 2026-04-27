package com.domisa.domisa_backend.profile.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
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

	@Transactional(readOnly = true)
	public Map<String, Boolean> checkNickname(String nickname) {
		validateNickname(nickname);
		return Map.of("isAvailable", !userRepository.existsByNickname(nickname));
	}

	@Transactional
	public Map<String, Long> registerProfile(Long userId, ProfileRegisterRequest request) {
		validateRequiredFields(request);
		validateNickname(request.nickName());

		if (userRepository.existsByNickname(request.nickName())) {
			throw new GlobalException(GlobalErrorCode.DUPLICATE_NICKNAME);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		user.registerProfile(
			request.nickName(),
			request.gender(),
			request.birthYear(),
			request.animalProfile(),
			request.contact(),
			request.inviteCode()
		);

		return Map.of("userId", user.getId());
	}

	private void validateRequiredFields(ProfileRegisterRequest request) {
		if (request.nickName() == null || request.nickName().isBlank()) {
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
