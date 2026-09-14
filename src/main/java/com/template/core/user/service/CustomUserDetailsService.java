package com.template.core.user.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.user.principal.UserPrincipal;
import com.template.core.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 로그인 ID(id)로 사용자를 조회해 {@link UserDetails}로 변환하는 인증 제공자.
 *
 * <p>Spring Security의 인증(로그인) 시 사용된다.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByLoginId(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "사용자를 찾을 수 없습니다. loginId=" + username));
    }
}