package com.template.core.common.code.dto;

import java.util.List;

import com.template.core.common.code.entity.CodeEntity;

/**
 * 계층형 코드 트리 노드 응답.
 *
 * <p>{@code children}이 비어 있으면 리프 노드다. 2레벨/3레벨 등 N계층이
 * 재귀적으로 중첩되어 표현된다.</p>
 */
public record CodeTreeResponse(String groupCode, String code, String parentCode, String name,
        String description, Integer sortOrder, boolean useYn, boolean seedYn,
        List<CodeTreeResponse> children) {

    /** 엔티티와 자식 노드 목록으로 트리 노드를 만든다. */
    public static CodeTreeResponse from(CodeEntity entity, List<CodeTreeResponse> children) {
        return new CodeTreeResponse(entity.getGroupCode(), entity.getCode(),
                entity.getParentCode(), entity.getName(), entity.getDescription(),
                entity.getSortOrder(), entity.isUseYn(), entity.isSeedYn(), children);
    }
}
