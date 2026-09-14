package com.template.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 애플리케이션 부트스트랩 스모크 테스트.
 *
 * <p>전체 컨텍스트 기동이 필요한 유일한 테스트로, 빈 구성 오류를
 * 조기에 잡는 용도로만 유지한다. 나머지 로직 검증은 각 슬라이스/단위 테스트에서 수행한다.</p>
 */
@SpringBootTest
class CoreApplicationTests {

    @Test
    @DisplayName("기본 프로파일로 애플리케이션 컨텍스트가 정상 기동된다")
    void contextLoads_WithDefaultProfile_StartsApplicationContext() {
        // given & when: 컨텍스트가 기동되었다(테스트 인스턴스 생성 시점)
        // then: 기동 실패 시 이 테스트가 실패한다
    }
}
