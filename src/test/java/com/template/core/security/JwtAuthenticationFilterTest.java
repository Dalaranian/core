package com.template.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.user.UserEntity;
import com.template.core.user.UserRepository;
import com.template.core.user.UserService;
import com.template.core.user.dto.UserJoinRequest;

/**
 * JwtAuthenticationFilter가 토큰 검증 시 회원 상태까지 확인하는지 검증하는 테스트.
 *
 * <p>활성화 회원의 토큰은 인증이 설정되고, 탈퇴 회원의 토큰은 인증이 설정되지 않는다.</p>
 */
@SpringBootTest
@Transactional
class JwtAuthenticationFilterTest {

    @Autowired
    private JwtAuthenticationFilter filter;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @AfterEach
    void 보안컨텍스트_정리() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 활성회원의_토큰이면_인증된다() throws Exception {
        userService.join(new UserJoinRequest("frank", "plain-pw", "프랭크"));
        UserEntity user = userRepository.findByLoginId("frank").orElseThrow();

        filter.doFilter(bearerRequest(jwtService.createToken(user)),
                new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("frank");
    }

    @Test
    void 탈퇴회원의_토큰이면_인증되지_않는다() throws Exception {
        userService.join(new UserJoinRequest("grace", "plain-pw", "그레이스"));
        UserEntity user = userRepository.findByLoginId("grace").orElseThrow();
        user.withdraw();

        filter.doFilter(bearerRequest(jwtService.createToken(user)),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /** Bearer 토큰을 담은 모의 요청을 만든다. */
    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
