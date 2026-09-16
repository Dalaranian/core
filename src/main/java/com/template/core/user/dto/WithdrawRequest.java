package com.template.core.user.dto;

import jakarta.validation.constraints.NotBlank;

/** 회원 탈퇴 요청 본문. 본인 확인용 비밀번호를 담는다. */
public record WithdrawRequest(@NotBlank(message = "비밀번호는 필수입니다.") String pw) {
}
