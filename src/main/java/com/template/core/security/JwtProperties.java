package com.template.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정값 바인딩 객체. application.yaml의 {@code jwt.*} 를 묶어 주입받는다.
 *
 * <p>yaml 키는 relaxed binding으로 매핑된다 (예: {@code expiry-seconds} → {@code expirySeconds}).
 * secret이 HS256 최소 길이(256bit)를 충족하지 못하면 JwtService 생성 시점에 시작이 실패한다.</p>
 *
 * @param secret        서명 시크릿 (HS256, 최소 32바이트)
 * @param expirySeconds 토큰 만료 시간(초)
 * @param issuer        발급자(iss) 클레임
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirySeconds, String issuer) {
}
