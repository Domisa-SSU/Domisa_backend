package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.config.DmsSessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class DmsAuthService {

	private static final String ADMIN_ID = "crewingfighting";
	private static final String ADMIN_PASSWORD = "domisado";
	private static final int DMS_SESSION_TIMEOUT_SECONDS = 60 * 60 * 12; // 12 hours

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
}
