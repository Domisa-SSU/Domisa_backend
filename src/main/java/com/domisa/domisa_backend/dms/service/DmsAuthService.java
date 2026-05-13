package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.config.DmsSessionKeys;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class DmsAuthService {

	private static final String ADMIN_ID = "crewingfighting";
	private static final String ADMIN_PASSWORD = "domisado";
	private static final int DMS_SESSION_TIMEOUT_SECONDS = 60 * 60 * 12; // 12 hours
	private static final String DMS_ADMIN_COOKIE_NAME = "DMS_ADMIN_AUTH";
	private static final String DMS_ADMIN_COOKIE_VALUE = "domisa-dms-admin-ok";

	public boolean login(String loginId, String password, HttpSession session) {
		if (!ADMIN_ID.equals(loginId) || !ADMIN_PASSWORD.equals(password)) {
			return false;
		}
		session.setAttribute(DmsSessionKeys.DMS_ADMIN_LOGIN, true);
		session.setMaxInactiveInterval(DMS_SESSION_TIMEOUT_SECONDS);
		return true;
	}

	public void logout(HttpSession session) {
		if (session != null) {
			session.invalidate();
		}
	}

	public String buildAuthCookieHeader() {
		return ResponseCookie.from(DMS_ADMIN_COOKIE_NAME, DMS_ADMIN_COOKIE_VALUE)
			.httpOnly(true)
			.secure(false)
			.sameSite("Lax")
			.path("/dms-room")
			.maxAge(Duration.ofHours(12))
			.build()
			.toString();
	}

	public String buildClearAuthCookieHeader() {
		return ResponseCookie.from(DMS_ADMIN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(false)
			.sameSite("Lax")
			.path("/dms-room")
			.maxAge(Duration.ZERO)
			.build()
			.toString();
	}

	public boolean isValidAuthCookie(String value) {
		return DMS_ADMIN_COOKIE_VALUE.equals(value);
	}

	public String authCookieName() {
		return DMS_ADMIN_COOKIE_NAME;
	}
}
