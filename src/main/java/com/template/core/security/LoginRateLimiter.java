package com.template.core.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.template.core.common.error.TooManyAttemptsException;

import lombok.RequiredArgsConstructor;

/**
 * 로그인 무차별 대입(브루트포스) 방지를 위한 계정 단위 시도 제한기.
 *
 * <p>로그인 ID별로 실패 시각을 슬라이딩 윈도우에 기록하고,
 * 윈도우({@code login.rate-limit.window-seconds}) 내 실패 횟수가
 * {@code login.rate-limit.max-attempts}에 도달하면 추가 로그인을 거부한다.
 * 가장 오래된 실패가 윈도우 밖으로 밀려날 때까지 잠금 상태가 유지된다.</p>
 *
 * <p>ponytail: 인메모리(단일 인스턴스) 구현. 현재 SQLite 단일 인스턴스 배포와
 * 일치한다. 멀티 인스턴스 배포 시 Redis 등 공유 저장소로 교체한다(업그레이드 경로).</p>
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    /**
     * ponytail: 추적 계정 수 상한. 존재하지 않는 랜덤 ID로 무차별 요청을 보내면
     * 실패 기록 맵이 무한 증식해 메모리 고갈(DoS)이 될 수 있으므로, 상한 초과 시
     * 전체 기록을 비우고 다시 시작한다(fail-open). 업그레이드 경로: Redis + TTL.
     */
    private static final int MAX_TRACKED_ACCOUNTS = 100_000;

    private final LoginRateLimitProperties props;

    /** 로그인 ID → 실패 시각(에포크 밀리초, 시간 오름차순) 목록. */
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    /**
     * 로그인 시도가 잠금 상태인지 확인한다.
     *
     * @param loginId 로그인 ID
     * @throws TooManyAttemptsException 윈도우 내 실패 횟수가 한도에 도달한 경우
     */
    public void check(String loginId) {
        Deque<Long> attempts = failures.get(loginId);
        if (attempts == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long windowMillis = props.windowSeconds() * 1000;
        synchronized (attempts) {
            // 시간 오름차순이므로 윈도우 밖 실패를 앞에서부터 제거한다
            while (!attempts.isEmpty() && attempts.peekFirst() < now - windowMillis) {
                attempts.pollFirst();
            }
            if (attempts.isEmpty()) {
                failures.remove(loginId);
                return;
            }
            if (attempts.size() >= props.maxAttempts()) {
                // 가장 오래된 실패가 윈도우 밖으로 나갈 때까지 대기
                long retryAfterSeconds = (attempts.peekFirst() + windowMillis - now + 999) / 1000;
                throw new TooManyAttemptsException(
                        "로그인 시도가 너무 많습니다. " + retryAfterSeconds + "초 후 다시 시도해 주세요. id=" + loginId);
            }
        }
    }

    /** 로그인 실패를 기록한다. */
    public void recordFailure(String loginId) {
        if (!failures.containsKey(loginId) && failures.size() >= MAX_TRACKED_ACCOUNTS) {
            failures.clear();
        }
        Deque<Long> attempts = failures.computeIfAbsent(loginId, key -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
        }
    }

    /** 로그인 성공 시 실패 기록을 초기화한다. */
    public void recordSuccess(String loginId) {
        failures.remove(loginId);
    }
}
