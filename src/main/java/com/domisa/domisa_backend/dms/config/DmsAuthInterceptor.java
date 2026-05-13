package com.domisa.domisa_backend.dms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DmsAuthInterceptor implements HandlerInterceptor {

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

		response.sendRedirect("/dms-room/login");
		return false;
	}
}
