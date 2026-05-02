package com.domisa.domisa_backend.auth.oauth;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class KakaoOAuthService {

	@Value("${kakao.client-id}")
	private String clientId;

	@Value("${kakao.client-secret}")
	private String clientSecret;

	@Value("${kakao.redirect-uri}")
	private String redirectUri;

	@Value("${kakao.allowed-redirect-uris}")
	private List<String> allowedRedirectUris;

	private final RestTemplate restTemplate = new RestTemplate();

	public String getAccessToken(String authorizationCode, String requestedRedirectUri) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String resolvedRedirectUri = resolveRedirectUri(requestedRedirectUri);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", clientId);
		body.add("redirect_uri", resolvedRedirectUri);
		body.add("code", authorizationCode);
		body.add("client_secret", clientSecret);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

		ResponseEntity<Map> response = restTemplate.postForEntity(
			"https://kauth.kakao.com/oauth/token",
			request,
			Map.class
		);

		return (String) response.getBody().get("access_token");
	}

	private String resolveRedirectUri(String requestedRedirectUri) {
		if (requestedRedirectUri == null || requestedRedirectUri.isBlank()) {
			return redirectUri;
		}

		String normalizedRedirectUri = requestedRedirectUri.trim();
		if (allowedRedirectUris.stream().map(String::trim).noneMatch(normalizedRedirectUri::equals)) {
			throw new GlobalException(GlobalErrorCode.INVALID_KAKAO_REDIRECT_URI);
		}

		return normalizedRedirectUri;
	}

	public Long getKakaoId(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<Map> response = restTemplate.exchange(
			"https://kapi.kakao.com/v2/user/me",
			HttpMethod.GET,
			request,
			Map.class
		);

		return Long.valueOf(String.valueOf(response.getBody().get("id")));
	}
}
