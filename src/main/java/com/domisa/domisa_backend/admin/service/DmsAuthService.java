package com.domisa.domisa_backend.admin.service;

import com.domisa.domisa_backend.admin.config.DmsSessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class DmsAuthService {

	private static final String ADMIN_ID = "crewingfighting";
	private static final String ADMIN_PASSWORD = "domisado";

	public boolean login(String loginId, String password, HttpSession session) {
		if (!ADMIN_ID.equals(loginId) || !ADMIN_PASSWORD.equals(password)) {
			return false;
		}
		session.setAttribute(DmsSessionKeys.DMS_ADMIN_LOGIN, true);
		return true;
	}

	public void logout(HttpSession session) {
		if (session != null) {
			session.invalidate();
		}
	}
}
