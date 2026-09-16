package com.template.core.common.code.dto;

/**
 * 코드 그룹 생성 요청.
 *
 * @param groupCode   그룹 식별자(유일)
 * @param groupName   그룹 표시 이름
 * @param description 그룹 설명
 */
public record CodeGroupCreateRequest(String groupCode, String groupName, String description) {
}
