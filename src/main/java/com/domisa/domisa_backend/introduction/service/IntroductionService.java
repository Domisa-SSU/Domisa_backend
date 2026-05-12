package com.domisa.domisa_backend.introduction.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.introduction.dto.IntroductionResponse;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntroductionService {

	private static final long INTRODUCER_REFERRAL_REWARD_COOKIES = 2L;

	private final IntroductionRepository introductionRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final CookieTransactionRepository cookieTransactionRepository;

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
				participant.setHasIntroduction(true);
				return;
			}
			throw new GlobalException(GlobalErrorCode.INTRODUCTION_ALREADY_ACCEPTED);
		}

		// 기존 소개서가 있으면 먼저 연결을 해제하고 새 소개서로 갈아탄다.
		Introduction currentIntroduction = participant.getIntroduction();
		boolean isFirstIntroductionSignup = currentIntroduction == null;
		if (currentIntroduction != null && !currentIntroduction.getId().equals(introduction.getId())) {
			currentIntroduction.clearParticipant();
		}

		introduction.assignParticipant(participant);
		if (isFirstIntroductionSignup) {
			rewardIntroducer(introduction, participant);
		}
	}

	private void rewardIntroducer(Introduction introduction, User participant) {
		User introducer = introduction.getIntroducer();
		boolean isSelfIntroduction = introducer != null && introducer.getId().equals(participant.getId());
		if (isSelfIntroduction) {
			return;
		}
		boolean hasValidIntroducer = introducer != null && !introducer.getId().equals(participant.getId());

		if (hasValidIntroducer) {
			introducer.addCookies(INTRODUCER_REFERRAL_REWARD_COOKIES);
			cookieTransactionRepository.save(CookieTransaction.reward(
				introducer,
				Math.toIntExact(INTRODUCER_REFERRAL_REWARD_COOKIES),
				"소개서 작성 보상"
			));
			notificationService.createNotification(NotificationType.REFERRAL, introducer.getId(), participant.getId());
		}
	}
}
