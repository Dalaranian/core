package com.template.core.common.error;

/**
 * 로그인 무차별 대입(브루트포스) 방지 한도 초과 예외.
 *
 * <p>슬라이딩 윈도우 내 로그인 실패 횟수가 한도에 도달한 계정에서
 * 추가 로그인 시도가 있을 때 발생하며, 429 Too Many Requests로 매핑된다.</p>
 */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String message) {
        super(message);
    }
}
