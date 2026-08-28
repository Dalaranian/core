package com.template.core.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 회원 상태.
 *
 * <p>DB에는 enum 이름/순번이 아닌 코드값(10/20)으로 저장된다.
 * 변환은 {@link CodeConverter}가 담당한다.</p>
 */
public enum UserStatus {

    /** 활성화 회원 */
    ACTIVE(10, "활성화회원"),

    /** 탈퇴 회원 */
    WITHDRAWN(20, "탈퇴회원");

    private final int code;
    private final String description;

    UserStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /** DB 저장용 코드값 */
    public int getCode() {
        return code;
    }

    /** 상태 설명 */
    public String getDescription() {
        return description;
    }

    /** 코드값으로 역변환한다. 알 수 없는 코드는 null을 반환한다. */
    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * DB 코드값(Integer) ↔ {@link UserStatus} 변환기.
     *
     * <p>DB에는 10/20 같은 코드값이 저장되고, 읽을 때 다시 enum으로 되돌린다.</p>
     */
    @Converter
    public static class CodeConverter implements AttributeConverter<UserStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(UserStatus attribute) {
            return attribute == null ? null : attribute.code;
        }

        @Override
        public UserStatus convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : UserStatus.fromCode(dbData);
        }
    }
}
