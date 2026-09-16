package com.template.core.common.code.dto;

import java.util.List;

import com.template.core.common.code.entity.CodeGroupEntity;

/**
 * 그룹 + 그룹 내 코드 트리 응답.
 *
 * @param group   그룹 정보
 * @param codes   그룹의 코드 트리(1레벨 목록, children에 하위 레벨이 중첩됨)
 */
public record CodeGroupResponse(CodeGroupEntity group, List<CodeTreeResponse> codes) {
}
