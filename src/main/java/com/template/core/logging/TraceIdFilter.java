package com.template.core.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP 요청마다 새 UUID(traceId)를 MDC에 넣는 필터.
 * 요청 단위로 로그를 묶어 추적하기 위함이며, 응답이 끝나면 반드시 MDC에서 제거한다.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter implements Ordered {

    /** 로그 패턴에서 %X{traceId} 로 참조하는 MDC 키 */
    public static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());
            filterChain.doFilter(request, response);
        } finally {
            // 스레드 재사용으로 인한 오염 방지를 위해 어떤 경로로 끝나든 제거한다.
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /** 보안 필터 체인보다 먼저 실행되어 초기 인증 로그에도 traceId가 붙도록 한다. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}