package com.domisa.domisa_backend.auth.filter;

import com.domisa.domisa_backend.auth.blacklist.UserBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBlacklistFilter extends OncePerRequestFilter {

	private final UserBlacklistService userBlacklistService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return "/dms-room".equals(uri) || uri.startsWith("/dms-room/") || uri.startsWith("/dms/");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Long userId = extractUserId();
		if (userId == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			if (userBlacklistService.isBlacklisted(userId)) {
				log.warn("블랙리스트 사용자 요청을 차단했습니다. userId={}, method={}, uri={}",
					userId, request.getMethod(), request.getRequestURI());
				writeErrorResponse(
					response,
					HttpStatus.FORBIDDEN,
					"BLACKLISTED_USER",
					"서비스 이용이 제한된 사용자입니다."
				);
				return;
			}
		} catch (DataAccessException exception) {
			log.error("블랙리스트 Redis 조회에 실패했습니다. userId={}", userId, exception);
			writeErrorResponse(
				response,
				HttpStatus.SERVICE_UNAVAILABLE,
				"BLACKLIST_CHECK_UNAVAILABLE",
				"서비스 이용 제한 상태를 확인할 수 없습니다."
			);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private Long extractUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof Long userId) {
			return userId;
		}
		if (principal instanceof Integer userId) {
			return userId.longValue();
		}
		if (principal instanceof String userId && !"anonymousUser".equals(userId)) {
			try {
				return Long.valueOf(userId);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private void writeErrorResponse(
		HttpServletResponse response,
		HttpStatus status,
		String code,
		String message
	) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("""
			{"status":%d,"code":"%s","message":"%s"}
			""".formatted(status.value(), code, message));
	}
}
