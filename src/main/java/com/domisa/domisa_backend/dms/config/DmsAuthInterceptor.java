package com.domisa.domisa_backend.dms.config;

import com.domisa.domisa_backend.dms.service.DmsAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class DmsAuthInterceptor implements HandlerInterceptor {

	private final DmsAuthService dmsAuthService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
		throws Exception {
		String path = request.getRequestURI();
		if ("/dms-room/login".equals(path)) {
			return true;
		}

		HttpSession session = request.getSession(false);
		if (session != null && Boolean.TRUE.equals(session.getAttribute(DmsSessionKeys.DMS_ADMIN_LOGIN))) {
			return true;
		}

		if (hasValidAuthCookie(request)) {
			HttpSession ensuredSession = request.getSession(true);
			ensuredSession.setAttribute(DmsSessionKeys.DMS_ADMIN_LOGIN, true);
			return true;
		}

		response.sendRedirect("/dms-room/login");
		return false;
	}

	private boolean hasValidAuthCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return false;
		}
		String authCookieName = dmsAuthService.authCookieName();
		for (Cookie cookie : cookies) {
			if (authCookieName.equals(cookie.getName()) && dmsAuthService.isValidAuthCookie(cookie.getValue())) {
				return true;
			}
		}
		return false;
	}
}
