package com.template.core.user.dto;

/**
 * 회원 가입 요청 DTO.
 *
 * @param id       로그인 ID
 * @param pw       비밀번호 (평문, 서비스에서 BCrypt로 인코딩해 저장)
 * @param userName 사용자 이름
 */
public record UserJoinRequest(String id, String pw, String userName) {
}