package com.domisa.domisa_backend.dms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class DmsWebConfig implements WebMvcConfigurer {

	private final DmsAuthInterceptor dmsAuthInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(dmsAuthInterceptor)
			.addPathPatterns("/dms-room/**")
			.excludePathPatterns("/dms-room/login");
	}
}
