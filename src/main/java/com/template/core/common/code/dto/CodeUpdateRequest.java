package com.template.core.common.code.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 코드 수정 요청. null 필드는 변경하지 않는다.
 *
 * <p>{@code seedYn=true}(enum 유래) 코드에는 적용할 수 없다.</p>
 *
 * <p>부분 수정 DTO이므로 @NotBlank는 쓰지 않고, 값이 있는 경우에만
 * 유효한 규칙(@Size, @Min — null이면 통과)만 적용한다.</p>
 *
 * @param name        코드명
 * @param description 코드 설명
 * @param sortOrder   정렬 순서
 * @param useYn       사용 여부
 */
public record CodeUpdateRequest(
		@Size(max = 100, message = "코드명은 100자 이하여야 합니다.") String name,
		@Size(max = 200, message = "설명은 200자 이하여야 합니다.") String description,
		@Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.") Integer sortOrder,
		Boolean useYn) {
}
