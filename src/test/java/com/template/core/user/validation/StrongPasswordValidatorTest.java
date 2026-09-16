package com.template.core.user.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.template.core.user.dto.UserJoinRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * StrongPasswordValidator의 비밀번호 정책 규칙을 검증한다.
 */
class StrongPasswordValidatorTest {

    private static final Validator validator;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Set<ConstraintViolation<UserJoinRequest>> validate(String id, String pw) {
        return validator.validate(new UserJoinRequest(id, pw, "홍길동"));
    }

    @Test
    @DisplayName("정책을 만족하는 비밀번호는 통과한다")
    void validPassword_Passes() {
        assertThat(validate("alice", "!Secure2026x")).isEmpty();
    }

    @Test
    @DisplayName("8자 미만은 위반한다")
    void tooShort_Fails() {
        assertThat(validate("alice", "Aa1!abc"))
                .anyMatch(v -> v.getMessage().contains("8자 이상"));
    }

    @Test
    @DisplayName("문자 조합이 2종 이하면 위반한다")
    void insufficientComposition_Fails() {
        assertThat(validate("alice", "acegik14"))
                .anyMatch(v -> v.getMessage().contains("3종류 이상"));
    }

    @Test
    @DisplayName("연속 문자(abc, 432)는 위반한다")
    void sequentialChars_Fails() {
        assertThat(validate("alice", "Abcd!2026x"))
                .anyMatch(v -> v.getMessage().contains("연속된 문자"));
        assertThat(validate("alice", "A543!2026x"))
                .anyMatch(v -> v.getMessage().contains("연속된 문자"));
    }

    @Test
    @DisplayName("반복 문자(aaa, 111)는 위반한다")
    void repeatedChars_Fails() {
        assertThat(validate("alice", "Aaa!2026xy"))
                .anyMatch(v -> v.getMessage().contains("3회 이상 연속"));
    }

    @Test
    @DisplayName("취약 사전 단어 포함 시 위반한다")
    void blacklistedWord_Fails() {
        assertThat(validate("alice", "xPassword1!"))
                .anyMatch(v -> v.getMessage().contains("쉽게 추측할 수 있는"));
    }

    @Test
    @DisplayName("비밀번호에 로그인 ID 포함 시 위반한다")
    void containsId_Fails() {
        assertThat(validate("alice", "alice!2026Ax"))
                .anyMatch(v -> v.getMessage().contains("로그인 ID를 포함"));
    }

    @Test
    @DisplayName("pw가 null이면 @NotBlank가 담당하므로 정책 검사는 통과한다")
    void nullPassword_Skipped() {
        assertThat(validate("alice", null))
                .noneMatch(v -> "pw".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("보안 정책"));
    }
}