package com.template.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * UserEntity가 실제 DB(SQLite)에 저장될 때 시퀀스(PK: seq)가
 * 자동 발급되는지 검증하는 통합 테스트.
 *
 * <p>트랜잭션을 롤백하여 실제 DB 데이터가 남지 않도록 한다.</p>
 */
@SpringBootTest
@Transactional
class UserEntityTest {

    @Autowired
    private EntityManager em;

    @Test
    void 저장시_시퀀스_자동발급() {
        UserEntity user = new UserEntity("test-user", "plain-pw", "홍길동");

        em.persist(user);
        em.flush();

        assertThat(user.getSeq()).isNotNull().isPositive();
    }
}