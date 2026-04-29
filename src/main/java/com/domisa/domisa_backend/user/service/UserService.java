package com.domisa.domisa_backend.user.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.dto.ContactDTO;
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

	// 내 정보 조회(마이페이지용)
	@Transactional(readOnly = true)
	public UserMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		return new UserMeResponse(
				user.getId(),
				user.getNickname(),
				user.getBirthYear(),
				user.getGender(),
				user.getAnimalProfile(),
				new ContactDTO(user.getContactType(), user.getContact()),
				user.getInviteCode()
		);
	}
}
