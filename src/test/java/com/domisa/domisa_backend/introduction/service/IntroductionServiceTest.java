package com.domisa.domisa_backend.introduction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IntroductionServiceTest {

	@Mock
	private IntroductionRepository introductionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private CookieTransactionRepository cookieTransactionRepository;

	@InjectMocks
	private IntroductionService introductionService;

	@Test
	void acceptIntroductionRewardsParticipantAndIntroducerOnFirstIntroductionSignup() {
		User participant = createUser(1L, 10L);
		User introducer = createUser(2L, 20L);
		Introduction introduction = createIntroduction(100L, introducer);

		when(userRepository.findById(1L)).thenReturn(Optional.of(participant));
		when(introductionRepository.findById(100L)).thenReturn(Optional.of(introduction));

		introductionService.acceptIntroduction(100L, participant);

		assertThat(participant.getIntroduction()).isSameAs(introduction);
		assertThat(participant.hasIntroduction()).isTrue();
		assertThat(participant.getCookieBalance()).isEqualTo(11L);
		assertThat(introducer.getCookieBalance()).isEqualTo(22L);
		verify(notificationService).createNotification(NotificationType.SIGNUP, 1L, null);
		verify(notificationService).createNotification(NotificationType.REFERRAL, 2L, 1L);
		ArgumentCaptor<CookieTransaction> transactionCaptor = ArgumentCaptor.forClass(CookieTransaction.class);
		verify(cookieTransactionRepository, times(2)).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getAllValues())
			.extracting(CookieTransaction::getAmount)
			.containsExactly(2, 1);
	}

	@Test
	void acceptIntroductionDoesNotRewardAgainWhenParticipantAlreadyOwnsSameIntroduction() {
		User participant = createUser(1L, 10L);
		User introducer = createUser(2L, 20L);
		Introduction introduction = createIntroduction(100L, introducer);
		introduction.assignParticipant(participant);
		participant.setHasIntroduction(false);

		when(userRepository.findById(1L)).thenReturn(Optional.of(participant));
		when(introductionRepository.findById(100L)).thenReturn(Optional.of(introduction));

		introductionService.acceptIntroduction(100L, participant);

		assertThat(participant.hasIntroduction()).isTrue();
		assertThat(participant.getCookieBalance()).isEqualTo(10L);
		assertThat(introducer.getCookieBalance()).isEqualTo(20L);
		verifyNoInteractions(notificationService);
		verifyNoInteractions(cookieTransactionRepository);
	}

	@Test
	void acceptIntroductionDoesNotRewardWhenChangingExistingIntroduction() {
		User participant = createUser(1L, 10L);
		User previousIntroducer = createUser(2L, 20L);
		User newIntroducer = createUser(3L, 30L);
		Introduction previousIntroduction = createIntroduction(99L, previousIntroducer);
		Introduction newIntroduction = createIntroduction(100L, newIntroducer);
		previousIntroduction.assignParticipant(participant);

		when(userRepository.findById(1L)).thenReturn(Optional.of(participant));
		when(introductionRepository.findById(100L)).thenReturn(Optional.of(newIntroduction));

		introductionService.acceptIntroduction(100L, participant);

		assertThat(previousIntroduction.getParticipant()).isNull();
		assertThat(participant.getIntroduction()).isSameAs(newIntroduction);
		assertThat(participant.getCookieBalance()).isEqualTo(10L);
		assertThat(newIntroducer.getCookieBalance()).isEqualTo(30L);
		verifyNoInteractions(notificationService);
		verifyNoInteractions(cookieTransactionRepository);
	}

	private Introduction createIntroduction(Long id, User introducer) {
		Introduction introduction = Introduction.create("q1", "q2", "q3", introducer, "LINK" + id);
		ReflectionTestUtils.setField(introduction, "id", id);
		return introduction;
	}

	private User createUser(Long id, Long cookies) {
		User user = User.create(id);
		user.setId(id);
		user.setCookies(cookies);
		user.setPublicId("USER" + id);
		return user;
	}
}
