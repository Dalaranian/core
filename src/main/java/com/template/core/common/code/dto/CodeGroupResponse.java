package com.template.core.common.code.dto;

import java.util.List;

import com.template.core.common.code.entity.CodeGroupEntity;

/**
 * 그룹 + 그룹 내 코드 트리 응답.
 *
 * <p>엔티티를 그대로 노출하지 않고 응답 필드만 제한해 담는다.</p>
 *
 * @param groupCode   그룹 식별자
 * @param groupName   그룹 표시 이름
 * @param description 그룹 설명
 * @param useYn       사용 여부
 * @param seedYn      enum 유래 시드 그룹 여부(수정·삭제 불가)
 * @param codes       그룹의 코드 트리(1레벨 목록, children에 하위 레벨이 중첩됨)
 */
public record CodeGroupResponse(String groupCode, String groupName, String description,
        boolean useYn, boolean seedYn, List<CodeTreeResponse> codes) {

    /** 그룹 엔티티와 코드 트리로 응답 DTO를 생성한다. */
    public static CodeGroupResponse from(CodeGroupEntity group, List<CodeTreeResponse> codes) {
        return new CodeGroupResponse(group.getGroupCode(), group.getGroupName(),
                group.getDescription(), group.isUseYn(), group.isSeedYn(), codes);
    }
}
