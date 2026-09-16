package com.template.core.user.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.template.core.user.entity.UserEntity;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 사용자 엔티티 JPA 리포지토리.
 *
 * <p>기본 키는 {@link UserEntity#getSeq()} 이므로, 로그인 ID(id) 조회/중복 확인은
 * JPQL로 명시해 기본 제공되는 findById(PK)와 충돌하지 않도록 한다.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /** 로그인 ID(id)로 사용자를 조회한다. */
    @Query("select u from UserEntity u where u.id = :loginId")
    Optional<UserEntity> findByLoginId(@Param("loginId") String loginId);

    /** 로그인 ID(id)가 이미 존재하는지 확인한다. */
    @Query("select count(u) > 0 from UserEntity u where u.id = :loginId")
    boolean existsByLoginId(@Param("loginId") String loginId);

    /**
     * 탈퇴 상태이면서 지정 시점(cutoff) 이전에 탈퇴 신청한 회원을 삭제한다.
     *
     * @param cutoff 삭제 대상 기준 시각. 이 시각보다 이전에 탈퇴 신청한 회원만 삭제한다.
     * @return 삭제된 건수
     */
    @Modifying
    @Query("delete from UserEntity u where u.status = com.template.core.user.code.UserStatus.WITHDRAWN"
            + " and u.withdrawnAt < :cutoff")
    int deleteWithdrawnBefore(@Param("cutoff") LocalDateTime cutoff);
}