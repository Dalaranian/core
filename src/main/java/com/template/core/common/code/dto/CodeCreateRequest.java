package com.template.core.common.code.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 코드 생성 요청.
 *
 * @param code        생성할 코드값(같은 그룹 내 유일)
 * @param parentCode  상위 코드값. null이면 그룹 1레벨, 지정하면 2레벨 이상
 * @param name        코드명
 * @param description 코드 설명
 * @param sortOrder   정렬 순서(미지정 시 0)
 */
public record CodeCreateRequest(
		@NotBlank(message = "코드값은 필수입니다.") @Size(max = 50, message = "코드값은 50자 이하여야 합니다.") String code,
		@Size(max = 50, message = "상위 코드값은 50자 이하여야 합니다.") String parentCode,
		@NotBlank(message = "코드명은 필수입니다.") @Size(max = 100, message = "코드명은 100자 이하여야 합니다.") String name,
		@Size(max = 200, message = "설명은 200자 이하여야 합니다.") String description,
		@Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.") Integer sortOrder) {
}
