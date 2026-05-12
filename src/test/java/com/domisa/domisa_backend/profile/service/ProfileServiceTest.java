package com.domisa.domisa_backend.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private IntroductionRepository introductionRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private CookieTransactionRepository cookieTransactionRepository;

	@InjectMocks
	private ProfileService profileService;

	@Test
	void registerProfileGrantsSignupRewardCookiesOnce() {
		User user = User.create(1234L);
		user.setId(1L);
		user.setPublicId("USER001");
		when(userRepository.existsByNickname("수빈")).thenReturn(false);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.count()).thenReturn(1L);
		when(introductionRepository.existsByParticipantId(1L)).thenReturn(false);

		ProfileRegisterResponse response = profileService.registerProfile(
			1L,
			new ProfileRegisterRequest("수빈", false, 2004L, AnimalProfile.CAT)
		);

		assertThat(user.getCookieBalance()).isEqualTo(3L);
		assertThat(user.getIsRegistered()).isTrue();
		assertThat(response.publicId()).isEqualTo("USER001");
		verify(notificationService).createNotification(NotificationType.SIGNUP, 1L, null);
		ArgumentCaptor<CookieTransaction> transactionCaptor = ArgumentCaptor.forClass(CookieTransaction.class);
		verify(cookieTransactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getAmount()).isEqualTo(3);
	}

	@Test
	void registerProfileDoesNotGrantSignupRewardAgainWhenAlreadyRegistered() {
		User user = User.create(1234L);
		user.setId(1L);
		user.setPublicId("USER001");
		user.setCookies(7L);
		user.setIsRegistered(true);
		when(userRepository.existsByNickname("수빈")).thenReturn(false);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.count()).thenReturn(1L);
		when(introductionRepository.existsByParticipantId(1L)).thenReturn(false);

		profileService.registerProfile(
			1L,
			new ProfileRegisterRequest("수빈", false, 2004L, AnimalProfile.CAT)
		);

		assertThat(user.getCookieBalance()).isEqualTo(7L);
		verifyNoInteractions(notificationService);
		verifyNoInteractions(cookieTransactionRepository);
	}
}
