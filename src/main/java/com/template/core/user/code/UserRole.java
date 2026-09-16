package com.template.core.user.code;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;

/**
 * 사용자 역할.
 *
 * <p>DB에는 enum 이름/순번이 아닌 코드값(10/20)으로 저장된다.
 * 변환은 {@link CodeConverter}가 담당한다.</p>
 *
 * <p>Spring Security 권한 문자열이 필요하면 {@link #getAuthority()}를 사용한다.
 * 새 역할 추가 시 코드값만 이어서 부여하면 된다.</p>
 */
@Getter
public enum UserRole {

    /** 일반 사용자 */
    ROLE_USER(10),

    /** 관리자 */
    ROLE_ADMIN(20);

    /**
     * -- GETTER --
     * DB 저장용 코드값
     */
    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    /** Spring Security 권한 문자열(= enum 이름) */
    public String getAuthority() {
        return name();
    }

    /** 코드값으로 역변환한다. 알 수 없는 코드는 null을 반환한다. */
    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return null;
    }

    /**
     * DB 코드값(Integer) ↔ {@link UserRole} 변환기.
     *
     * <p>DB에는 10/20 같은 코드값이 저장되고, 읽을 때 다시 enum으로 되돌린다.</p>
     */
    @Converter
    public static class CodeConverter implements AttributeConverter<UserRole, Integer> {

        @Override
        public Integer convertToDatabaseColumn(UserRole attribute) {
            return attribute == null ? null : attribute.code;
        }

        @Override
        public UserRole convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : UserRole.fromCode(dbData);
        }
    }
}