package com.domisa.domisa_backend.introduction.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.introduction.dto.IntroductionResponse;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntroductionService {

	private final IntroductionRepository introductionRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public IntroductionResponse getIntroductionByLinkCode(String linkCode) {
		Introduction introduction = introductionRepository.findByLinkCode(linkCode)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.INTRODUCTION_NOT_FOUND));
		return IntroductionResponse.from(introduction);
	}

	@Transactional
	public void acceptIntroduction(Long introductionId, User user) {
		User participant = userRepository.findById(user.getId())
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		Introduction introduction = introductionRepository.findById(introductionId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.INTRODUCTION_NOT_FOUND));

		// 소개서의 등록된 사람이 있으면
		if (introduction.getParticipant() != null) {
			if (introduction.getParticipant().getId().equals(participant.getId())) {
				return;
			}
			throw new GlobalException(GlobalErrorCode.INTRODUCTION_ALREADY_ACCEPTED);
		}

		// 기존 소개서가 있으면 먼저 연결을 해제하고 새 소개서로 갈아탄다.
		Introduction currentIntroduction = participant.getIntroduction();
		if (currentIntroduction != null && !currentIntroduction.getId().equals(introduction.getId())) {
			currentIntroduction.clearParticipant();
		}

		// 유저에도 등록, 소개서에서도 유저 등록
		introduction.assignParticipant(participant);
	}
}
