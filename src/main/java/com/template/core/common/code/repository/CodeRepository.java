package com.template.core.common.code.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.template.core.common.code.entity.CodeEntity;
import com.template.core.common.code.entity.CodeId;

/**
 * 공통 코드 리포지토리.
 *
 * <p>복합키(group_code + code) 기반이며, 계층 조회에 필요한 메서드만 제공한다.
 * PK 외 컬럼 조회는 JPQL {@code @Query}로 명시한다.</p>
 */
@Repository
public interface CodeRepository extends JpaRepository<CodeEntity, CodeId> {

    /** 그룹 내 특정 코드를 조회한다(복합 PK 조회). */
    java.util.Optional<CodeEntity> findByGroupCodeAndCode(String groupCode, String code);

    /** 그룹 내 코드 존재 여부(복합 PK 조회). */
    boolean existsByGroupCodeAndCode(String groupCode, String code);

    /** 활성 상태인 자식 코드 존재 여부(비활성화 전 하위 코드 확인용). */
    @Query("select count(c) > 0 from CodeEntity c where c.groupCode = :groupCode"
            + " and c.parentCode = :parentCode and c.useYn = true")
    boolean existsByGroupCodeAndParentCodeAndUseYnTrue(@Param("groupCode") String groupCode,
            @Param("parentCode") String parentCode);
}
