package com.template.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

/**
 * UserEntity의 JPA 매핑(PK 시퀀스, 상태 컨버터) 검증 테스트.
 *
 * <p>전체 컨텍스트 대신 {@link DataJpaTest} 슬라이스를 사용해
 * JPA 계층만 빠르게 기동한다. DB는 임시 SQLite 파일을 사용하고
 * create-drop으로 스키마를 매번 새로 만들어 반복 가능하다(Repeatable).</p>
 */
@DataJpaTest
// 내장 H2 대체를 비활성화하고 실제 SQLite 방언을 그대로 사용한다
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:build/test-userEntity.db",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect"
})
class UserEntityTest {

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("저장 시 시퀀스(PK)가 자동 발급된다")
    void save_WithNewUser_GeneratesSequenceId() {
        // given: PK 없는 신규 사용자를 준비한다
        UserEntity user = UserEntity.builder()
                .id("test-user")
                .pw("plain-pw")
                .userName("홍길동")
                .build();

        // when: 영속화하고 DB에 반영한다
        em.persist(user);
        em.flush();

        // then: PK가 자동 발급된다
        assertThat(user.getSeq()).isNotNull().isPositive();
    }

    @Test
    @DisplayName("신규 사용자는 기본 상태 ACTIVE(코드 10)로 저장된다")
    void save_WithNewUser_PersistsActiveStatusAsCode10() {
        // given: 상태 미지정(기본 ACTIVE) 사용자를 준비한다
        UserEntity user = UserEntity.builder()
                .id("status-user")
                .pw("plain-pw")
                .userName("홍길동")
                .build();

        // when: 영속화하고 DB에 반영한다
        em.persist(user);
        em.flush();

        // then: 저장된 엔티티의 상태가 ACTIVE(코드 10)인지 확인한다
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getStatus().getCode()).isEqualTo(10);
    }

    @Test
    @DisplayName("상태 컨버터가 ACTIVE를 코드 10으로 변환한다")
    void codeConverter_WithActive_ConvertsToCode10() {
        // given: 상태 컨버터를 준비한다
        var converter = new UserStatus.CodeConverter();

        // when & then: ACTIVE ↔ 10 변환이 대칭으로 성립한다
        assertThat(converter.convertToDatabaseColumn(UserStatus.ACTIVE)).isEqualTo(10);
        assertThat(converter.convertToDatabaseColumn(UserStatus.WITHDRAWN)).isEqualTo(20);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(10)).isEqualTo(UserStatus.ACTIVE);
        assertThat(converter.convertToEntityAttribute(20)).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("fromCode에 알 수 없는 코드를 넣으면 null을 반환한다")
    void fromCode_WithUnknownCode_ReturnsNull() {
        // when & then: 알 수 없는 코드는 안전하게 null 처리된다
        assertThat(UserStatus.fromCode(999)).isNull();
    }
}
