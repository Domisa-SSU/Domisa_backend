package com.domisa.domisa_backend.global.auth.oauth;

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

	private final RestTemplate restTemplate = new RestTemplate();

	public String getAccessToken(String authorizationCode) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", clientId);
		body.add("redirect_uri", redirectUri);
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
