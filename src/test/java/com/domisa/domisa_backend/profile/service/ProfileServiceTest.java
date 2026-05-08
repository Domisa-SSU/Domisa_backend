package com.domisa.domisa_backend.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterResponse;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private IntroductionRepository introductionRepository;

	@InjectMocks
	private ProfileService profileService;

	@Test
	void registerProfileDoesNotGrantSignupRewardCookies() {
		User user = User.create(1234L);
		user.setId(1L);
		user.setPublicId("USER001");
		user.setCookies(5L);
		when(userRepository.existsByNickname("수빈")).thenReturn(false);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.count()).thenReturn(1L);
		when(introductionRepository.existsByParticipantId(1L)).thenReturn(false);

		ProfileRegisterResponse response = profileService.registerProfile(
			1L,
			new ProfileRegisterRequest("수빈", false, 2004L, AnimalProfile.CAT)
		);

		assertThat(user.getCookieBalance()).isEqualTo(5L);
		assertThat(user.getIsRegistered()).isTrue();
		assertThat(response.publicId()).isEqualTo("USER001");
	}
}
