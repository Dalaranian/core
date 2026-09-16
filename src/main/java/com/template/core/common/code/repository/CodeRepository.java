package com.template.core.common.code.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.template.core.common.code.entity.CodeEntity;
import com.template.core.common.code.entity.CodeId;

/**
 * 공통 코드 리포지토리.
 *
 * <p>복합키(group_code + code) 기반이며, 계층 조회에 필요한 메서드만 제공한다.</p>
 */
public interface CodeRepository extends JpaRepository<CodeEntity, CodeId> {

    /** 그룹의 모든 코드(활성/비활성 포함)를 정렬 순서대로 조회한다. */
    List<CodeEntity> findByGroupCodeOrderBySortOrderAsc(String groupCode);

    /** 그룹 내 특정 코드를 조회한다. */
    java.util.Optional<CodeEntity> findByGroupCodeAndCode(String groupCode, String code);

    /** 그룹 내 코드 존재 여부. */
    boolean existsByGroupCodeAndCode(String groupCode, String code);

    /** 활성 상태인 자식 코드 존재 여부(비활성화 전 하위 코드 확인용). */
    boolean existsByGroupCodeAndParentCodeAndUseYnTrue(String groupCode, String parentCode);
}
