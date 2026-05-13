package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.dto.DmsIntroductionResponse;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmsIntroductionService {

	private final IntroductionRepository introductionRepository;

	@Transactional(readOnly = true)
	public DmsIntroductionResponse getIntroductions() {
		return new DmsIntroductionResponse(
			introductionRepository.findAllForDms().stream()
				.map(this::toRow)
				.toList()
		);
	}

	@Transactional
	public void deleteIntroduction(Long introductionId) {
		Introduction introduction = introductionRepository.findById(introductionId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.INTRODUCTION_NOT_FOUND));
		introduction.clearParticipant();
		introductionRepository.delete(introduction);
	}

	private DmsIntroductionResponse.IntroductionRow toRow(Introduction introduction) {
		User introducer = introduction.getIntroducer();
		User participant = introduction.getParticipant();
		return new DmsIntroductionResponse.IntroductionRow(
			introduction.getId(),
			introducer == null ? null : introducer.getId(),
			introducer == null ? null : introducer.getPublicId(),
			introducer == null ? null : introducer.getNickname(),
			participant == null ? null : participant.getId(),
			participant == null ? null : participant.getPublicId(),
			participant == null ? null : participant.getNickname(),
			introduction.getLinkCode(),
			introduction.getQ1(),
			introduction.getQ2(),
			introduction.getQ3()
		);
	}
}
