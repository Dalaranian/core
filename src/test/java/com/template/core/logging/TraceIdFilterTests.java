package com.template.core.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTests {

    @Test
    void assignsFreshUuidPerRequestAndNeverLeaksIntoNextRequest() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        List<String> traceIds = new ArrayList<>();

        // 요청 내부에서는 traceId가 채워져 있어야 한다.
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> traceIds.add(MDC.get(TraceIdFilter.TRACE_ID_KEY)));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> traceIds.add(MDC.get(TraceIdFilter.TRACE_ID_KEY)));

        // 각 요청마다 서로 다른 UUID가 부여되고,
        assertThat(traceIds).hasSize(2)
                .allSatisfy(id -> assertThat(id).isNotBlank());
        assertThat(traceIds.get(0)).isNotEqualTo(traceIds.get(1));

        // 요청이 끝난 뒤에는 MDC에서 완전히 제거되어 스레드 간/요청 간 오염이 없어야 한다.
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
    }
}