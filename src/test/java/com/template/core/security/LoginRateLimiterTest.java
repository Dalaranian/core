package com.template.core.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.template.core.common.error.TooManyAttemptsException;

/**
 * LoginRateLimiter 단위 테스트.
 *
 * <p>윈도우를 1초로 설정하고 짧은 sleep으로 윈도우 경과를 시뮬레이션해
 * 외부 의존성 없이 슬라이딩 윈도우 동작을 검증한다.</p>
 */
class LoginRateLimiterTest {

    /** 한도 3회, 윈도우 1초. */
    private final LoginRateLimiter limiter = new LoginRateLimiter(new LoginRateLimitProperties(3, 1));

    @Test
    @DisplayName("실패 기록이 없으면 체크가 통과한다")
    void check_WithNoFailures_Passes() {
        // given: 실패 기록이 없는 계정

        // when & then: 체크가 예외 없이 통과한다
        assertThatCode(() -> limiter.check("alice")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("윈도우 내 실패 횟수가 한도에 도달하면 예외가 발생한다")
    void check_WithMaxFailuresInWindow_Throws() {
        // given: 한도(3회)만큼 실패를 기록한다
        limiter.recordFailure("bob");
        limiter.recordFailure("bob");
        limiter.recordFailure("bob");

        // when & then: TooManyAttemptsException이 발생한다
        assertThatThrownBy(() -> limiter.check("bob"))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("로그인 시도가 너무 많습니다");
    }

    @Test
    @DisplayName("윈도우를 벗어난 실패는 카운트되지 않는다")
    void check_WithFailuresOutsideWindow_Passes() throws InterruptedException {
        // given: 한도(3회) 중 2회를 기록한 뒤 윈도우(1초)가 경과한다
        limiter.recordFailure("carol");
        limiter.recordFailure("carol");
        Thread.sleep(1100);

        // when & then: 윈도우 밖 실패는 카운트되지 않아 체크가 통과한다
        assertThatCode(() -> limiter.check("carol")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 기록이 초기화된다")
    void recordSuccess_ClearsFailures() {
        // given: 한도 직전(2회)까지 실패를 기록한 뒤 성공한다
        limiter.recordFailure("dave");
        limiter.recordFailure("dave");
        limiter.recordSuccess("dave");

        // when & then: 기록이 초기화되어 한도(3회)에 도달하지 않는다
        limiter.recordFailure("dave");
        limiter.recordFailure("dave");
        assertThatCode(() -> limiter.check("dave")).doesNotThrowAnyException();
    }
}
