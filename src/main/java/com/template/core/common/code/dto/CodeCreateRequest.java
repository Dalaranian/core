package com.template.core.common.code.dto;

/**
 * 코드 생성 요청.
 *
 * @param code        생성할 코드값(같은 그룹 내 유일)
 * @param parentCode  상위 코드값. null이면 그룹 1레벨, 지정하면 2레벨 이상
 * @param name        코드명
 * @param description 코드 설명
 * @param sortOrder   정렬 순서(미지정 시 0)
 */
public record CodeCreateRequest(String code, String parentCode, String name,
        String description, Integer sortOrder) {
}
