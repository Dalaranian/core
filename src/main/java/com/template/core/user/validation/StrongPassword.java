package com.template.core.user.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 비밀번호 보안 정책 검증 제약.
 *
 * <p>클래스 레벨로 부여하며, 대상 record의 {@code id()}와 {@code pw()}를 사용해
 * 개인정보(ID 포함) 규칙까지 교차 검증한다. 상세 규칙은
 * {@link StrongPasswordValidator} 참고.</p>
 *
 * <p>현재 {@link com.template.core.user.dto.UserJoinRequest} 전용이다.
 * 비밀번호 정책은 가입 시에만 검사한다(로그인·탈퇴 시 검사하면
 * 정책 강화 시 기존 회원이 잠기고 포맷 정보가 유출된다).</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

	String message() default "비밀번호가 보안 정책을 만족하지 않습니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}