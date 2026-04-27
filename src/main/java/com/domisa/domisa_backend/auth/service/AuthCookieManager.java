package com.domisa.domisa_backend.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieManager {

	private final boolean secure;
	private final String sameSite;
	private final String domain;

	public AuthCookieManager(
		@Value("${app.auth.cookie.secure:false}") boolean secure,
		@Value("${app.auth.cookie.same-site:Lax}") String sameSite,
		@Value("${app.auth.cookie.domain:}") String domain
	) {
		this.secure = secure;
		this.sameSite = sameSite;
		this.domain = domain == null ? "" : domain.trim();
	}

	public void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite(sameSite)
			.path("/")
			.maxAge(maxAge);

		if (!domain.isBlank()) {
			builder.domain(domain);
		}

		response.addHeader("Set-Cookie", builder.build().toString());
	}

	public void expireCookie(HttpServletResponse response, String name) {
		addCookie(response, name, "", Duration.ZERO);
	}
}
