package com.template.core.common.code.entity;

import com.template.core.common.code.CodeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 코드 엔티티(codes 테이블).
 *
 * <p>{@code parentCode} 자기참조(같은 그룹 내 상위 코드)로 계층 구조를 표현한다.
 * parentCode가 null이면 그룹 1레벨, 상위 코드를 가리키면 2레벨 이상이다.
 * 생성·수정 시 서비스가 같은 그룹 내 상위 코드 존재 여부를 검증하므로
 * 순환 참조와 타 그룹 참조는 발생하지 않는다.</p>
 *
 * <p>{@code seedYn=true}인 코드는 enum({@link CodeEnum})에서 유래한 것으로
 * 기동 시 싱크가 원본이 되며, 관리 API로 수정·비활성화할 수 없다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@IdClass(CodeId.class)
@Table(name = "codes")
public class CodeEntity {

    /** 소속 그룹 식별자(복합 PK 1) */
    @Id
    @Column(name = "group_code", nullable = false, length = 30)
    private String groupCode;

    /** 코드값(복합 PK 2). enum 코드는 "10"처럼 문자열화되어 저장된다. */
    @Id
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** 상위 코드값. null이면 그룹 1레벨. 2레벨/3레벨 등 N계층을 지원한다. */
    @Column(name = "parent_code", length = 50)
    private String parentCode;

    /** 코드명(예: ACTIVE) */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 코드 설명(예: 활성화회원) */
    @Column(name = "description", length = 200)
    private String description;

    /** 정렬 순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 사용 여부. false면 비즈니스 조회(트리/단건)에서 제외한다. */
    @Builder.Default
    @Column(name = "use_yn", nullable = false, columnDefinition = "boolean default true")
    private boolean useYn = true;

    /** enum에서 유래한 시드 코드 여부. true면 관리 API로 수정·비활성화할 수 없다. */
    @Builder.Default
    @Column(name = "seed_yn", nullable = false, columnDefinition = "boolean default false")
    private boolean seedYn = false;

    /** enum 시드 코드를 생성한다. */
    public static CodeEntity seed(String groupCode, CodeEnum source) {
        return CodeEntity.builder()
                .groupCode(groupCode)
                .code(source.getCodeValue())
                .parentCode(source.getParentCode())
                .name(source.getName())
                .description(source.getDescription())
                .sortOrder(source.getSortOrder())
                .seedYn(true)
                .build();
    }

    /** enum 시드 코드의 name/description/sortOrder를 원본 enum 값에 맞춰 갱신한다. */
    public void syncFrom(CodeEnum source) {
        this.name = source.getName();
        this.description = source.getDescription();
        this.sortOrder = source.getSortOrder();
    }

    /** 코드명을 변경한다(관리 API용). */
    public void updateName(String name) {
        this.name = name;
    }

    /** 코드 설명을 변경한다(관리 API용). */
    public void updateDescription(String description) {
        this.description = description;
    }

    /** 정렬 순서를 변경한다(관리 API용). */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 사용 여부를 변경한다(관리 API용 soft delete 포함). */
    public void updateUseYn(boolean useYn) {
        this.useYn = useYn;
    }
}
