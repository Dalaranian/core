package com.template.core.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO.
 *
 * <p>포맷 정보 유출 방지를 위해 필수 여부(@NotBlank)만 검사하고
 * 크기·조합 규칙은 검사하지 않는다. 비밀번호 정책은 가입 시에만 적용한다.</p>
 *
 * @param id 로그인 ID
 * @param pw 비밀번호 (평문, 서비스에서 BCrypt 해시와 대조)
 */
public record LoginRequest(
		@NotBlank(message = "아이디는 필수입니다.") String id,
		@NotBlank(message = "비밀번호는 필수입니다.") String pw) {
}
