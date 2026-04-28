package com.domisa.domisa_backend.introduction.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.introduction.dto.IntroductionResponse;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntroductionService {

	private final IntroductionRepository introductionRepository;

	@Transactional(readOnly = true)
	public IntroductionResponse getIntroductionByLinkCode(String linkCode) {
		Introduction introduction = introductionRepository.findByLinkCode(linkCode)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.INTRODUCTION_NOT_FOUND));
		return IntroductionResponse.from(introduction);
	}

	@Transactional
	public void acceptIntroduction(Long introductionId, User user) {
		Introduction introduction = introductionRepository.findById(introductionId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.INTRODUCTION_NOT_FOUND));

		// 소개서의 등록된 사람이 있으면
		if (introduction.getParticipant() != null) {
			if (introduction.getParticipant().getId().equals(user.getId())) {
				return;
			}
			throw new GlobalException(GlobalErrorCode.INTRODUCTION_ALREADY_ACCEPTED);
		}

		// 유저가 이미 소개서가 있으면 문제
		if (user.hasIntroduction()) {
			throw new GlobalException(GlobalErrorCode.USER_ALREADY_HAS_INTRODUCTION);
		}
		// 유저에도 등록, 소개서에서도 유저 등록
		introduction.assignParticipant(user);
	}
}
