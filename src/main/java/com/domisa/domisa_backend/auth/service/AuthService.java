package com.domisa.domisa_backend.auth.service;

import com.domisa.domisa_backend.auth.dto.AuthMeResponse;
import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.jwt.JwtProvider;
import com.domisa.domisa_backend.auth.oauth.KakaoOAuthService;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.util.UserPublicIdGenerator;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MAX_PUBLIC_ID_GENERATION_ATTEMPTS = 5;

	private final KakaoOAuthService kakaoOAuthService;
	private final UserRepository userRepository;
	private final JwtProvider jwtProvider;
	private final AuthCookieManager authCookieManager;

	public LoginResponse login(String authorizationCode, HttpServletResponse response) {
		String kakaoAccessToken = kakaoOAuthService.getAccessToken(authorizationCode);
		Long kakaoId = kakaoOAuthService.getKakaoId(kakaoAccessToken);

		User user = userRepository.findByKakaoId(kakaoId)
			.orElseGet(() -> createUserWithPublicId(kakaoId));

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
				user.hasIntroduction(),
				user.hasCard()
			)
		);
	}

	public java.util.Map<String, String> logout(HttpServletResponse response) {
		authCookieManager.expireCookie(response, "accessToken");
		authCookieManager.expireCookie(response, "refreshToken");
		return java.util.Map.of("message", "Successfully logged out");
	}

	public AuthMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

		return new AuthMeResponse(
			user.getPublicId(),
			Math.toIntExact(user.getCookies()),
			new AuthMeResponse.StatusDto(
				user.getIsRegistered(),
				user.hasIntroduction(),
				user.hasCard()
			)
		);
	}

	private User createUserWithPublicId(Long kakaoId) {
		for (int attempt = 0; attempt < MAX_PUBLIC_ID_GENERATION_ATTEMPTS; attempt++) {
			User user = User.create(kakaoId);
			user.setPublicId(generateUniquePublicId());

			try {
				return userRepository.saveAndFlush(user);
			} catch (DataIntegrityViolationException exception) {
				if (isPublicIdCollision(exception)) {
					continue;
				}
				throw exception;
			}
		}

		throw new GlobalException(GlobalErrorCode.USER_PUBLIC_ID_GENERATION_FAILED);
	}

	private String generateUniquePublicId() {
		String publicId;
		do {
			publicId = UserPublicIdGenerator.generate();
		} while (userRepository.existsByPublicId(publicId));
		return publicId;
	}

	private boolean isPublicIdCollision(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			String message = current.getMessage();
			if (message != null) {
				String normalizedMessage = message.toLowerCase();
				if (normalizedMessage.contains("public_id") || normalizedMessage.contains("uk_users_public_id")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
