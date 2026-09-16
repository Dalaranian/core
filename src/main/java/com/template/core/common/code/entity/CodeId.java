package com.template.core.common.code.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link CodeEntity}의 복합 기본키(group_code + code).
 *
 * <p>같은 코드값이어도 그룹이 다르면 공존할 수 있어야 하므로 복합키로 정의한다.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CodeId implements Serializable {

    private String groupCode;
    private String code;
}
