package com.template.core.user.dto;

import com.template.core.user.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 가입 요청 DTO.
 *
 * <p>비밀번호 보안 정책은 {@link StrongPassword} 클래스 레벨 제약으로 검증한다.</p>
 *
 * @param id       로그인 ID
 * @param pw       비밀번호 (평문, 서비스에서 BCrypt로 인코딩해 저장)
 * @param userName 사용자 이름
 */
@StrongPassword
public record UserJoinRequest(
		@NotBlank(message = "아이디는 필수입니다.") @Size(max = 50, message = "아이디는 50자 이하여야 합니다.") String id,
		@NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.") String pw,
		@NotBlank(message = "이름은 필수입니다.") @Size(max = 50, message = "이름은 50자 이하여야 합니다.") String userName) {
}