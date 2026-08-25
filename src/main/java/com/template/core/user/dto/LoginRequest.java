package com.template.core.user.dto;

/**
 * 로그인 요청 DTO.
 *
 * @param id 로그인 ID
 * @param pw 비밀번호 (평문, 서비스에서 BCrypt 해시와 대조)
 */
public record LoginRequest(String id, String pw) {
}
