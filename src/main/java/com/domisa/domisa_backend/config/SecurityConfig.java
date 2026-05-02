package com.domisa.domisa_backend.config;

import com.domisa.domisa_backend.auth.filter.JwtAuthenticationFilter;
import com.domisa.domisa_backend.auth.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final List<String> ALLOWED_ORIGINS = List.of(
        "https://domisa.vercel.app",
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:8080"
    );

    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
	                .requestMatchers("/health").permitAll()
	                .requestMatchers("/api/introduction/**").permitAll()
	                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
	                .requestMatchers("/api/webhooks/payaction/**").permitAll()
	                .requestMatchers(
	                    "/v3/api-docs/**",
	                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
	    public CorsConfigurationSource corsConfigurationSource() {
	        CorsConfiguration config = new CorsConfiguration();
            List<String> allowedOrigins = frontendUrl == null || frontendUrl.isBlank()
                ? ALLOWED_ORIGINS
                : java.util.stream.Stream.concat(ALLOWED_ORIGINS.stream(), java.util.stream.Stream.of(frontendUrl))
                    .distinct()
                    .toList();
	        config.setAllowedOrigins(allowedOrigins);
	        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
	        config.setAllowedHeaders(List.of("*"));
            config.setExposedHeaders(List.of("*"));
	        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
