package com.template.core.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTests {

    @Test
    @DisplayName("요청마다 새 traceId를 부여하고 종료 시 MDC를 정리한다")
    void doFilter_WithEachRequest_AssignsUniqueTraceIdAndClearsMdc() throws Exception {
        // given: 필터와 traceId 수집기를 준비한다
        TraceIdFilter filter = new TraceIdFilter();
        List<String> traceIds = new ArrayList<>();

        // when: 두 개의 요청을 순차 실행하며 요청 내부의 traceId를 수집한다
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> traceIds.add(MDC.get(TraceIdFilter.TRACE_ID_KEY)));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> traceIds.add(MDC.get(TraceIdFilter.TRACE_ID_KEY)));

        // then: 각 요청마다 서로 다른 traceId가 부여되고,
        assertThat(traceIds).hasSize(2)
                .allSatisfy(id -> assertThat(id).isNotBlank());
        assertThat(traceIds.get(0)).isNotEqualTo(traceIds.get(1));

        // then: 요청 종료 후 MDC가 완전히 정리되어 스레드 간 오염이 없다
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
    }
}