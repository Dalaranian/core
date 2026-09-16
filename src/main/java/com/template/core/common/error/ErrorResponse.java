package com.template.core.common.error;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 에러 정보. 공통 응답 봉투(ApiResponse)의 error 필드에 담긴다.
 *
 * @param code        비즈니스 에러 코드 (예: "INVALID_ARGUMENT")
 * @param message     클라이언트에 노출되는 안내 메시지
 * @param fieldErrors 필드별 검증 오류 (Bean Validation 실패 시에만 존재, 그 외 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    /**
     * Bean Validation 실패용. 필드명 → 오류 메시지 맵을 함께 담는다.
     */
    public static ErrorResponse ofValidation(String code, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors);
    }
}
