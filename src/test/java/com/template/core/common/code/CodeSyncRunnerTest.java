package com.template.core.common.code;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.template.core.common.code.repository.CodeGroupRepository;
import com.template.core.common.code.repository.CodeRepository;

/**
 * CodeSyncRunner의 enum → 코드 테이블 싱크 검증 테스트.
 *
 * <p>UserEntityTest와 동일한 임시 SQLite + create-drop 슬라이스로 기동한다.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(CodeSyncRunner.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:build/test-codeSync.db",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect"
})
class CodeSyncRunnerTest {

    @Autowired
    private CodeSyncRunner runner;

    @Autowired
    private CodeGroupRepository codeGroupRepository;

    @Autowired
    private CodeRepository codeRepository;

    @Test
    @DisplayName("기동 싱크 시 enum 값들이 시드 그룹/코드로 저장된다")
    void run_PersistsEnumSeeds() {
        // when: 싱크를 실행한다
        runner.run(null);

        // then: USER_STATUS 그룹과 ACTIVE("10") 코드가 시드로 저장된다
        assertThat(codeGroupRepository.findByGroupCode("USER_STATUS"))
                .isPresent()
                .hasValueSatisfying(group -> {
                    assertThat(group.getGroupName()).isEqualTo("USER_STATUS");
                    assertThat(group.isSeedYn()).isTrue();
                });
        assertThat(codeRepository.findByGroupCodeAndCode("USER_STATUS", "10"))
                .isPresent()
                .hasValueSatisfying(code -> {
                    assertThat(code.getName()).isEqualTo("ACTIVE");
                    assertThat(code.getDescription()).isEqualTo("활성화회원");
                    assertThat(code.isSeedYn()).isTrue();
                    assertThat(code.getParentCode()).isNull();
                });
        // USER_ROLE 그룹도 시드된다
        assertThat(codeRepository.findByGroupCodeAndCode("USER_ROLE", "10"))
                .isPresent()
                .hasValueSatisfying(code -> assertThat(code.getDescription()).isEqualTo("일반 사용자"));
        // enum 상수 4개 = 시드 코드 4건
        assertThat(codeRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("싱크를 반복 실행해도 중복 생성되지 않는다(멱등)")
    void run_Twice_IsIdempotent() {
        // given: 한 번 싱크한 상태
        runner.run(null);
        long firstCount = codeRepository.count();

        // when: 다시 싱크한다
        runner.run(null);

        // then: 건수가 변하지 않는다
        assertThat(codeRepository.count()).isEqualTo(firstCount);
    }
}
