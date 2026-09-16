package com.template.core.common.code.dto;

/**
 * 코드 수정 요청. null 필드는 변경하지 않는다.
 *
 * <p>{@code seedYn=true}(enum 유래) 코드에는 적용할 수 없다.</p>
 *
 * @param name        코드명
 * @param description 코드 설명
 * @param sortOrder   정렬 순서
 * @param useYn       사용 여부
 */
public record CodeUpdateRequest(String name, String description, Integer sortOrder, Boolean useYn) {
}
