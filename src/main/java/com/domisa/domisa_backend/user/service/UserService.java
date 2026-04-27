package com.domisa.domisa_backend.user.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.user.dto.UserMeResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final S3ObjectUrlService s3ObjectUrlService;

	@Transactional(readOnly = true)
	public UserMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		return new UserMeResponse(
			new UserMeResponse.UserDto(
				user.getId(),
				user.getNickname(),
				user.getAge(),
				user.getGenderDisplay(),
				s3ObjectUrlService.getProfileImageUrl(user),
				Math.toIntExact(user.getCookies()),
				user.getInviteCode()
			),
			new UserMeResponse.StatusDto(
				user.getIsRegistered(),
				user.hasIntroduction(),
				user.hasCard()
			)
		);
	}
}
