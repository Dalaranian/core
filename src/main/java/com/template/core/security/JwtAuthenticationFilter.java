package com.template.core.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.template.core.user.repository.UserRepository;
import com.template.core.user.entity.UserStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authorization 헤더의 Bearer JWT를 검증해 {@link SecurityContext}에 인증 정보를 설정하는 필터.
 *
 * <p>토큰 서명 검증에 더해 subject(로그인 ID)로 사용자를 조회해
 * 회원 상태가 활성화(ACTIVE)인 경우에만 인증 객체를 만들어 통과시키고,
 * 토큰이 없거나 유효하지 않거나 탈퇴 회원이면 인증을 설정하지 않은 채
 * 다음 필터로 넘겨 보안 규칙(authenticated)이 판단하게 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            String subject = jwtService.validateAndGetSubject(token);
            if (subject != null && isActivatedUser(subject)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        subject, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 로그인 ID로 사용자를 조회해 활성화 회원인지 확인한다.
     *
     * <p>사용자가 없거나 탈퇴(WITHDRAWN) 상태이면 false로,
     * 안전한 쪽인 기본 거부(default deny)로 처리한다.</p>
     */
    private boolean isActivatedUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }

    /** Authorization 헤더에서 "Bearer " 접두사를 제거한 토큰을 추출한다. */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
