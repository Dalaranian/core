package com.template.core.common.code.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.template.core.common.code.entity.CodeGroupEntity;

/**
 * 코드 그룹 리포지토리.
 */
@Repository
public interface CodeGroupRepository extends JpaRepository<CodeGroupEntity, String> {

    /** 그룹 식별자로 그룹을 조회한다. */
    java.util.Optional<CodeGroupEntity> findByGroupCode(String groupCode);

    /** 전체 그룹을 식별자 순으로 조회한다. */
    List<CodeGroupEntity> findAllByOrderByGroupCodeAsc();
}
