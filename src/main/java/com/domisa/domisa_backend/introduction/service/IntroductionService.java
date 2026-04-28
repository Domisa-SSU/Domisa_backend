package com.domisa.domisa_backend.introduction.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.introduction.dto.IntroductionResponse;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
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
}
