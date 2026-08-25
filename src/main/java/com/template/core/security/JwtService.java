package com.template.core.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.template.core.user.UserEntity;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 발급/검증 서비스.
 *
 * <p>로그인 성공 시 HS256으로 서명한 JWT를 발급하고, 이후 요청에서
 * Bearer 토큰을 검증해 사용자 식별자(subject=로그인 ID)를 추출한다.</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirySeconds;
    private final String issuer;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiry-seconds}") long expirySeconds,
                      @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = expirySeconds;
        this.issuer = issuer;
    }

    /** 로그인 성공한 사용자에게 JWT를 발급한다. */
    public String createToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId())
                .claim("seq", user.getSeq())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * JWT를 검증하고 subject(로그인 ID)를 반환한다.
     *
     * @return 유효하면 로그인 ID, 서명/만료/발급자 불일치 등 실패 시 null
     */
    public String validateAndGetSubject(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
