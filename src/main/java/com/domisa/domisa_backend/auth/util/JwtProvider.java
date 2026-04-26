package com.domisa.domisa_backend.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-in-seconds}") long accessSeconds,
            @Value("${jwt.refresh-token-validity-in-seconds}") long refreshSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessSeconds * 1000;
        this.refreshTokenValidity = refreshSeconds * 1000;
    }

    public String createAccessToken(Long userId) {
        return buildToken(String.valueOf(userId), accessTokenValidity);
    }

    public String createRefreshToken(Long userId) {
        return buildToken(String.valueOf(userId), refreshTokenValidity);
    }

    private String buildToken(String subject, long validity) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validity))
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessTokenValidityMs() { return accessTokenValidity; }
    public long getRefreshTokenValidityMs() { return refreshTokenValidity; }
}
