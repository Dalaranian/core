package com.template.core.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.template.core.common.response.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 서비스/컨트롤러 레이어에서 던진 예외를 공통 응답 봉투(ApiResponse)로 변환하는 핸들러.
 *
 * <p>비즈니스 예외(IllegalState/IllegalArgument)는 적절한 4xx 상태 코드로 매핑하고,
 * 그 외 알 수 없는 예외는 500으로 변환하되 내부 메시지는 로그로만 남긴다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 이미 사용 중인 로그인 ID 등 상태 충돌 → 409 Conflict. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_STATE", e.getMessage());
    }

    /** 사용자 없음, 비밀번호 불일치 등 잘못된 요청 → 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", e.getMessage());
    }

    /** Bean Validation 실패(@Valid) → 400. 필드별 오류 메시지를 함께 담는다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", fieldErrors);
    }

    /** 로그인 무차별 대입(브루트포스) 방지 한도 초과 → 429 Too Many Requests. */
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooManyAttempts(TooManyAttemptsException e) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", e.getMessage());
    }

    /** 폴백: 예상하지 못한 예외 → 500. 내부 정보 노출을 막기 위해 범용 메시지로 응답한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "서버에 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message) {
        return build(status, code, message, null);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message,
            Map<String, String> fieldErrors) {
        log.warn("클라이언트 오류 응답: code={}, message={}", code, message);
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ErrorResponse.ofValidation(code, message, fieldErrors)));
    }
}
