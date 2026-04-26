package com.domisa.domisa_backend.global.auth.resolver;

import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.domain.user.repository.UserRepository;
import com.domisa.domisa_backend.global.auth.annotation.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final UserRepository userRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(AuthUser.class)
			&& User.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}

		Long userId = extractUserId(authentication.getPrincipal());
		if (userId == null) {
			return null;
		}

		return userRepository.findById(userId).orElse(null);
	}

	private Long extractUserId(Object principal) {
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
}
