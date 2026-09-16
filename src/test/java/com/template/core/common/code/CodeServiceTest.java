package com.template.core.common.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.template.core.common.code.dto.CodeCreateRequest;
import com.template.core.common.code.dto.CodeGroupCreateRequest;
import com.template.core.common.code.dto.CodeTreeResponse;
import com.template.core.common.code.dto.CodeUpdateRequest;
import com.template.core.common.code.entity.CodeEntity;
import com.template.core.common.code.repository.CodeRepository;
import com.template.core.common.code.service.CodeService;

/**
 * CodeService의 계층 트리 조회와 관리(생성/수정/비활성화) 동작 검증 테스트.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(CodeService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:build/test-codeService.db",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect"
})
class CodeServiceTest {

    private static final String GROUP = "TEST_MENU";

    @Autowired
    private CodeService codeService;

    @Autowired
    private CodeRepository codeRepository;

    /** 그룹 + 3레벨 계층(100 → 110 → 111)과 비활성 코드(120)를 준비한다. */
    private void setUpThreeLevelHierarchy() {
        codeService.createGroup(new CodeGroupCreateRequest(GROUP, "테스트 메뉴", null));
        codeService.createCode(GROUP, new CodeCreateRequest("100", null, "대분류", null, 1));
        codeService.createCode(GROUP, new CodeCreateRequest("110", "100", "중분류", null, 1));
        codeService.createCode(GROUP, new CodeCreateRequest("111", "110", "소분류", null, 1));
        codeService.createCode(GROUP, new CodeCreateRequest("120", "100", "숨김분류", null, 2));
        codeService.updateCode(GROUP, "120", new CodeUpdateRequest(null, null, null, false));
    }

    @Test
    @DisplayName("parentCode 체인으로 3레벨 트리가 재귀적으로 조립된다")
    void getTree_BuildsThreeLevelHierarchy() {
        // given: 3레벨 계층을 준비한다
        setUpThreeLevelHierarchy();

        // when: 트리를 조회한다(비활성 제외)
        var tree = codeService.getTree(GROUP);

        // then: 1레벨 100 아래 110, 그 아래 111이 중첩된다. 비활성 120은 제외된다
        assertThat(tree).hasSize(1);
        var level1 = tree.get(0);
        assertThat(level1.code()).isEqualTo("100");
        assertThat(level1.children()).extracting(CodeTreeResponse::code).containsExactly("110");
        assertThat(level1.children().get(0).children())
                .extracting(CodeTreeResponse::code)
                .containsExactly("111");
        assertThat(level1.children().get(0).children().get(0).children()).isEmpty();
    }

    @Test
    @DisplayName("getChildren으로 특정 코드의 직속 자식만 조회한다")
    void getChildren_ReturnsDirectChildrenOnly() {
        // given: 3레벨 계층을 준비한다
        setUpThreeLevelHierarchy();

        // when & then: 100의 직속 자식은 활성인 110뿐이다(비활성 120 제외)
        assertThat(codeService.getChildren(GROUP, "100"))
                .extracting(CodeEntity::getCode)
                .containsExactly("110");
        assertThat(codeService.getChildren(GROUP, null))
                .extracting(CodeEntity::getCode)
                .containsExactly("100");
    }

    @Test
    @DisplayName("관리 화면용 트리는 비활성 코드도 포함한다")
    void getTree_WithIncludeDisabled_ContainsDisabledCode() {
        // given: 비활성 코드가 있는 계층을 준비한다
        setUpThreeLevelHierarchy();

        // when & then: includeDisabled=true이므로 비활성 120까지 100 아래에 정렬되어 나온다
        assertThat(codeService.getTree(GROUP, true))
                .extracting(CodeTreeResponse::code)
                .containsExactly("100");
        assertThat(codeService.getTree(GROUP, true).get(0).children())
                .extracting(CodeTreeResponse::code)
                .containsExactly("110", "120");
    }

    @Test
    @DisplayName("다른 그룹의 상위 코드를 parentCode로 지정하면 거부한다")
    void createCode_WithForeignGroupParent_Throws() {
        // given: 두 그룹을 준비한다
        codeService.createGroup(new CodeGroupCreateRequest("G1", "그룹1", null));
        codeService.createGroup(new CodeGroupCreateRequest("G2", "그룹2", null));
        codeService.createCode("G1", new CodeCreateRequest("10", null, "코드", null, 0));

        // when & then: G2 코드의 부모를 G1의 코드로 지정하면 실패한다
        assertThatThrownBy(() -> codeService.createCode("G2",
                new CodeCreateRequest("20", "10", "코드", null, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("활성 하위 코드가 남아 있으면 비활성화가 거부된다")
    void disableCode_WithActiveChild_Throws() {
        // given: 부모-자식 계층을 준비한다
        setUpThreeLevelHierarchy();

        // when & then: 100은 활성 자식 110이 있으므로 비활성화할 수 없다
        assertThatThrownBy(() -> codeService.disableCode(GROUP, "100"))
                .isInstanceOf(IllegalStateException.class);
        // 리프(111)는 비활성화할 수 있다
        codeService.disableCode(GROUP, "111");
        assertThat(codeRepository.findByGroupCodeAndCode(GROUP, "111"))
                .isPresent()
                .hasValueSatisfying(code -> assertThat(code.isUseYn()).isFalse());
        // 자식이 모두 비활성화된 110도 이제 비활성화할 수 있다
        codeService.disableCode(GROUP, "110");
        assertThat(codeRepository.findByGroupCodeAndCode(GROUP, "110"))
                .hasValueSatisfying(code -> assertThat(code.isUseYn()).isFalse());
    }

    @Test
    @DisplayName("동적 코드는 수정되고 조회 캐시도 갱신된다")
    void updateCode_AppliesChangesAndRefreshesCache() {
        // given: 계층을 준비하고 캐시를 워밍업한다
        setUpThreeLevelHierarchy();
        assertThat(codeService.getCode(GROUP, "110")).isPresent();

        // when: 110의 이름을 변경한다
        codeService.updateCode(GROUP, "110", new CodeUpdateRequest("변경된 중분류", null, null, null));

        // then: 캐시가 무효화되어 변경된 이름이 조회된다
        assertThat(codeService.getCode(GROUP, "110"))
                .isPresent()
                .hasValueSatisfying(code -> assertThat(code.getName()).isEqualTo("변경된 중분류"));
    }
}
