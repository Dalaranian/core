package com.template.core.common.code.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * code 도메인 요청 DTO의 Bean Validation 규칙을 검증한다.
 */
class CodeDtoValidationTest {

    private static final Validator validator;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("그룹 생성 요청: 필수 값 누락 시 위반한다")
    void groupCreate_BlankFields_Fail() {
        Set<ConstraintViolation<CodeGroupCreateRequest>> violations = validator.validate(
                new CodeGroupCreateRequest(" ", " ", null));
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("groupCode", "groupName");
    }

    @Test
    @DisplayName("코드 생성 요청: sortOrder 음수는 위반하고, 선택 필드 null은 통과한다")
    void codeCreate_NegativeSortOrder_Fails() {
        assertThat(validator.validate(new CodeCreateRequest("10", null, "활성", null, -1)))
                .anyMatch(v -> v.getMessage().contains("0 이상"));
        assertThat(validator.validate(new CodeCreateRequest("10", null, "활성", null, 0))).isEmpty();
    }

    @Test
    @DisplayName("코드 수정 요청: null은 통과하고, 값이 있으면 크기 규칙이 적용된다")
    void codeUpdate_NullFields_Pass() {
        assertThat(validator.validate(new CodeUpdateRequest(null, null, null, null))).isEmpty();
        assertThat(validator.validate(new CodeUpdateRequest("x".repeat(101), null, null, null)))
                .anyMatch(v -> v.getMessage().contains("100자 이하"));
    }
}