package com.domisa.domisa_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.jwt.JwtProvider;
import com.domisa.domisa_backend.auth.oauth.KakaoOAuthService;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private KakaoOAuthService kakaoOAuthService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtProvider jwtProvider;

	@Mock
	private AuthCookieManager authCookieManager;

	@Mock
	private HttpServletResponse response;

	@InjectMocks
	private AuthService authService;

	@Test
	void loginRetriesWhenPublicIdConstraintCollides() {
		when(kakaoOAuthService.getAccessToken("code")).thenReturn("kakao-token");
		when(kakaoOAuthService.getKakaoId("kakao-token")).thenReturn(1L);
		when(userRepository.findByKakaoId(1L)).thenReturn(Optional.empty());
		when(userRepository.existsByPublicId(anyString())).thenReturn(false);
		when(userRepository.saveAndFlush(any(User.class)))
			.thenThrow(new DataIntegrityViolationException("Duplicate entry for key public_id"))
			.thenAnswer(invocation -> {
				User user = invocation.getArgument(0);
				user.setId(10L);
				return user;
			});
		when(jwtProvider.createAccessToken(10L)).thenReturn("access-token");
		when(jwtProvider.createRefreshToken(10L)).thenReturn("refresh-token");
		when(jwtProvider.getAccessTokenValidityMs()).thenReturn(60_000L);
		when(jwtProvider.getRefreshTokenValidityMs()).thenReturn(120_000L);

		LoginResponse loginResponse = authService.login("code", response);

		assertThat(loginResponse.status().isRegistered()).isFalse();
		verify(userRepository, times(2)).saveAndFlush(any(User.class));
	}

	@Test
	void loginThrowsWhenPublicIdCollidesFiveTimes() {
		when(kakaoOAuthService.getAccessToken("code")).thenReturn("kakao-token");
		when(kakaoOAuthService.getKakaoId("kakao-token")).thenReturn(1L);
		when(userRepository.findByKakaoId(1L)).thenReturn(Optional.empty());
		when(userRepository.existsByPublicId(anyString())).thenReturn(false);
		when(userRepository.saveAndFlush(any(User.class)))
			.thenThrow(new DataIntegrityViolationException("Duplicate entry for key public_id"));

		assertThatThrownBy(() -> authService.login("code", response))
			.isInstanceOf(GlobalException.class)
			.satisfies(exception -> {
				GlobalException globalException = (GlobalException) exception;
				assertThat(globalException.getErrorCode()).isEqualTo(GlobalErrorCode.USER_PUBLIC_ID_GENERATION_FAILED);
			});
		verify(userRepository, times(5)).saveAndFlush(any(User.class));
	}
}
