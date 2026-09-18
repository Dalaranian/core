package com.template.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 무차별 대입(브루트포스) 방지 설정값 바인딩 객체.
 *
 * <p>application.yaml의 {@code login.rate-limit.*} 를 묶어 주입받는다.
 * yaml 키는 relaxed binding으로 매핑된다 (예: {@code window-seconds} → {@code windowSeconds}).</p>
 *
 * @param maxAttempts   슬라이딩 윈도우 내 허용하는 최대 로그인 실패 횟수
 * @param windowSeconds 슬라이딩 윈도우 길이(초)
 */
@ConfigurationProperties(prefix = "login.rate-limit")
public record LoginRateLimitProperties(int maxAttempts, long windowSeconds) {

    public LoginRateLimitProperties {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("login.rate-limit.max-attempts는 1 이상이어야 합니다.");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("login.rate-limit.window-seconds는 1 이상이어야 합니다.");
        }
    }
}
