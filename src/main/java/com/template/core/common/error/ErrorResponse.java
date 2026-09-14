package com.template.core.common.error;

/**
 * 에러 정보. 공통 응답 봉투(ApiResponse)의 error 필드에 담긴다.
 *
 * @param code    비즈니스 에러 코드 (예: "INVALID_ARGUMENT")
 * @param message 클라이언트에 노출되는 안내 메시지
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
