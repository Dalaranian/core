package com.template.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 필터 체인 설정.
 *
 * <p>JWT 기반 무상태(STATELESS) 인증을 사용하므로 세션·폼 로그인·HTTP Basic을 모두 끄고,
 * 인증 헤더의 Bearer 토큰을 검증하는 {@link JwtAuthenticationFilter}를 앞에 끼운다.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * HTTP 요청에 적용할 보안 필터 체인을 구성한다.
     *
     * @param http 필터 체인 빌더
     * @return 구성된 보안 필터 체인
     * @throws Exception 필터 체인 구성 중 예외 발생 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // JWT 인증만 사용하므로 폼 로그인/HTTP Basic/세션은 모두 비활성화한다.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/users", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 비밀번호 해시용 인코더. 회원 가입 시 {@code pw}를 평문이 아닌 BCrypt로 저장한다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
