package com.domisa.domisa_backend.auth.service;

import com.domisa.domisa_backend.auth.dto.LoginResponse;
import com.domisa.domisa_backend.auth.util.JwtProvider;
import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final KakaoOAuthService kakaoOAuthService;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public AuthService(KakaoOAuthService kakaoOAuthService,
                       UserRepository userRepository,
                       JwtProvider jwtProvider) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(String authorizationCode, HttpServletResponse response) {
        String kakaoAccessToken = kakaoOAuthService.getAccessToken(authorizationCode);
        String kakaoId = kakaoOAuthService.getKakaoId(kakaoAccessToken);

        User user = userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(User.create(kakaoId)));

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        addCookie(response, "accessToken", accessToken,
                (int) (jwtProvider.getAccessTokenValidityMs() / 1000));
        addCookie(response, "refreshToken", refreshToken,
                (int) (jwtProvider.getRefreshTokenValidityMs() / 1000));

        return new LoginResponse(
                new LoginResponse.StatusDto(
                        user.getIsRegistered(),
                        user.getHasIntroduction(),
                        user.getIsProfileCompleted()
                )
        );
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // 배포 시 아래 두 줄 주석 해제
        // cookie.setSecure(true);
        // cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
