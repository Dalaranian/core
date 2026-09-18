package com.template.core.user.dto;

/** 비밀번호 변경 요청 본문. 본인 확인용 기존 비밀번호와 새 비밀번호를 담는다. */
public record ChangePasswordRequest(String oldPw, String newPw) {
}
