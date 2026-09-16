package com.template.core.common.code.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코드 그룹 엔티티(code_groups 테이블).
 *
 * <p>그룹은 코드의 최상위 파티션 키이며, 그룹 아래로 {@link CodeEntity}가
 * parent_code 체인으로 N레벨 트리를 구성한다.</p>
 *
 * <p>{@code seedYn=true}인 그룹은 enum({@link CodeEnum})에서 유래한 것으로
 * 기동 시 싱크가 원본이 되며, 관리 API로 수정할 수 없다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "code_groups")
public class CodeGroupEntity {

    /** 그룹 식별자(예: USER_STATUS) */
    @Id
    @Column(name = "group_code", nullable = false, length = 30)
    private String groupCode;

    /** 그룹 표시 이름(예: 회원 상태) */
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    /** 그룹 설명 */
    @Column(name = "description", length = 200)
    private String description;

    /** 사용 여부. false면 해당 그룹의 코드를 비즈니스 조회에서 제외한다. */
    @Builder.Default
    @Column(name = "use_yn", nullable = false, columnDefinition = "boolean default true")
    private boolean useYn = true;

    /** enum에서 유래한 시드 그룹 여부. true면 관리 API로 수정·삭제할 수 없다. */
    @Builder.Default
    @Column(name = "seed_yn", nullable = false, columnDefinition = "boolean default false")
    private boolean seedYn = false;
}
