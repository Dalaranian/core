package com.template.core.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 본문. 본인 확인용 기존 비밀번호와 새 비밀번호를 담는다.
 *
 * <p>필수 여부 등 형식 검증은 DTO에서, 기존 비밀번호 대조·직전 비밀번호 동일 확인 등
 * DB 상태 비교 검증은 서비스에서 담당한다.</p>
 *
 * @param oldPw 기존 비밀번호 (평문, 서비스에서 BCrypt 해시와 대조)
 * @param newPw 새 비밀번호 (평문, 서비스에서 BCrypt로 인코딩해 저장)
 */
public record ChangePasswordRequest(
        @NotBlank(message = "기존 비밀번호는 필수입니다.") String oldPw,
        @NotBlank(message = "새 비밀번호는 필수입니다.") String newPw) {
}
