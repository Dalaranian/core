package com.template.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 필터 체인 설정.
 *
 * <p>아직 골격만 존재하고 필터 체인 구성 로직은 TODO로 남겨 둠.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * HTTP 요청에 적용할 보안 필터 체인을 구성한다.
     *
     * @param http 필터 체인 빌더
     * @return 구성된 보안 필터 체인
     * @throws Exception 필터 체인 구성 중 예외 발생 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO: csrf, authorizeHttpRequests, formLogin/httpBasic 등 필터 체인 구성
        return http.build();
    }

    /** 비밀번호 해시용 인코더. 회원 가입 시 {@code pw}를 평문이 아닌 BCrypt로 저장한다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}