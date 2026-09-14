package com.template.core.user.dto;

/** 회원 탈퇴 요청 본문. 본인 확인용 비밀번호를 담는다. */
public record WithdrawRequest(String pw) {
}
