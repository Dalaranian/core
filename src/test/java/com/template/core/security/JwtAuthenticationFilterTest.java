package com.template.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.template.core.user.UserEntity;
import com.template.core.user.UserRepository;
import com.template.core.user.UserStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JwtAuthenticationFilter 단위 테스트.
 *
 * <p>JwtService와 UserRepository를 Mockito로 격리해 DB/전체 컨텍스트 없이
 * 빠르게(Fast), 순서 의존 없이(Isolated) 검증한다.</p>
 *
 * <p>활성화 회원의 토큰은 인증이 설정되고, 탈퇴 회원·유효하지 않은 토큰·
 * 토큰 없음 요청은 인증이 설정되지 않은 채 다음 필터로 진행된다.</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void 보안컨텍스트_초기화() {
        // 테스트 간 SecurityContext 오염을 방지한다
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void 보안컨텍스트_정리() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("활성화 회원의 유효한 토큰이면 인증이 설정된다")
    void doFilter_WithValidTokenOfActiveUser_SetsAuthentication() throws Exception {
        // given: 유효한 토큰과 활성화(ACTIVE) 회원을 준비한다
        UserEntity activeUser = UserEntity.builder()
                .id("frank")
                .pw("pw")
                .userName("프랭크")
                .status(UserStatus.ACTIVE)
                .build();
        when(jwtService.validateAndGetSubject("valid-token")).thenReturn("frank");
        when(userRepository.findByLoginId("frank")).thenReturn(java.util.Optional.of(activeUser));

        // when: Bearer 토큰을 담은 요청으로 필터를 실행한다
        filter.doFilter(bearerRequest("valid-token"), new MockHttpServletResponse(), filterChain);

        // then: SecurityContext에 로그인 ID 기반 인증이 설정되고 체인이 계속 진행된다
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("frank");
        assertThat(authentication.getAuthorities())
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"));
        verify(filterChain).doFilter(
                org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("탈퇴 회원의 토큰이면 인증이 설정되지 않는다")
    void doFilter_WithTokenOfWithdrawnUser_DoesNotSetAuthentication() throws Exception {
        // given: 유효한 토큰이지만 탈퇴(WITHDRAWN) 상태 회원을 준비한다
        UserEntity withdrawnUser = UserEntity.builder()
                .id("grace")
                .pw("pw")
                .userName("그레이스")
                .status(UserStatus.WITHDRAWN)
                .build();
        when(jwtService.validateAndGetSubject("valid-token")).thenReturn("grace");
        when(userRepository.findByLoginId("grace")).thenReturn(java.util.Optional.of(withdrawnUser));

        // when: Bearer 토큰을 담은 요청으로 필터를 실행한다
        filter.doFilter(bearerRequest("valid-token"), new MockHttpServletResponse(), filterChain);

        // then: 인증은 설정되지 않은 채 체인이 진행된다
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(
                org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 없이 통과한다")
    void doFilter_WithInvalidToken_DoesNotSetAuthentication() throws Exception {
        // given: 토큰 검증이 실패한다(null 반환)
        when(jwtService.validateAndGetSubject("bad-token")).thenReturn(null);

        // when: Bearer 토큰을 담은 요청으로 필터를 실행한다
        filter.doFilter(bearerRequest("bad-token"), new MockHttpServletResponse(), filterChain);

        // then: 회원 조회 없이 인증 미설정 상태로 체인이 진행된다
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userRepository, never()).findByLoginId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 토큰 검증 없이 통과한다")
    void doFilter_WithoutAuthorizationHeader_DoesNotSetAuthentication() throws Exception {
        // given: 인증 헤더가 없는 요청을 준비한다
        HttpServletRequest request = new MockHttpServletRequest();

        // when: 필터를 실행한다
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        // then: 토큰 검증 없이 인증 미설정 상태로 체인이 진행된다
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).validateAndGetSubject(org.mockito.ArgumentMatchers.anyString());
    }

    /** Bearer 토큰을 담은 모의 요청을 만든다. */
    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
