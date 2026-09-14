package com.template.core.common.response;

import java.time.Instant;

import org.slf4j.MDC;

import com.template.core.common.error.ErrorResponse;
import com.template.core.logging.TraceIdFilter;

/**
 * 모든 API 응답이 사용하는 공통 봉투(envelope).
 *
 * @param success   성공 여부
 * @param data      정상 응답 DTO (실패 시 null)
 * @param error     에러 정보 (성공 시 null)
 * @param traceId   요청 추적용 식별자 (TraceIdFilter의 MDC 값)
 * @param timestamp 응답 생성 시각 (UTC)
 */
public record ApiResponse<T>(boolean success, T data, ErrorResponse error, String traceId, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, MDC.get(TraceIdFilter.TRACE_ID_KEY), Instant.now());
    }

    public static ApiResponse<Void> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error, MDC.get(TraceIdFilter.TRACE_ID_KEY), Instant.now());
    }
}
