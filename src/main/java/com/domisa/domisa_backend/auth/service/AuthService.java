package com.domisa.domisa_backend.auth.service;

import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.jwt.JwtProvider;
import com.domisa.domisa_backend.auth.oauth.KakaoOAuthService;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final KakaoOAuthService kakaoOAuthService;
	private final UserRepository userRepository;
	private final JwtProvider jwtProvider;
	private final AuthCookieManager authCookieManager;

	public LoginResponse login(String authorizationCode, HttpServletResponse response) {
		String kakaoAccessToken = kakaoOAuthService.getAccessToken(authorizationCode);
		Long kakaoId = kakaoOAuthService.getKakaoId(kakaoAccessToken);

		User user = userRepository.findByKakaoId(kakaoId)
			.orElseGet(() -> userRepository.save(User.create(kakaoId)));

		String accessToken = jwtProvider.createAccessToken(user.getId());
		String refreshToken = jwtProvider.createRefreshToken(user.getId());

		authCookieManager.addCookie(
			response,
			"accessToken",
			accessToken,
			Duration.ofMillis(jwtProvider.getAccessTokenValidityMs())
		);
		authCookieManager.addCookie(
			response,
			"refreshToken",
			refreshToken,
			Duration.ofMillis(jwtProvider.getRefreshTokenValidityMs())
		);

		return new LoginResponse(
			new LoginResponse.StatusDto(
				user.getIsRegistered(),
				user.getHasIntroduction(),
				user.getIsProfileCompleted()
			)
		);
	}

	public java.util.Map<String, String> logout(HttpServletResponse response) {
		authCookieManager.expireCookie(response, "accessToken");
		authCookieManager.expireCookie(response, "refreshToken");
		return java.util.Map.of("message", "Successfully logged out");
	}
}
