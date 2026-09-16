package com.template.core.user.principal;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.template.core.user.entity.UserEntity;
import com.template.core.user.code.UserRole;

import lombok.RequiredArgsConstructor;

/**
 * {@link UserEntity}를 Spring Security의 {@link UserDetails}로 감싸는 어댑터.
 *
 * <p>인증 식별자로 로그인 ID(id)를 사용하며, 권한은 사용자의 역할({@link UserRole})에서 매핑한다.</p>
 */
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UserEntity user;

    /** 로그인 ID(id)를 사용자명으로 사용한다. */
    @Override
    public String getUsername() {
        return user.getId();
    }

    @Override
    public String getPassword() {
        return user.getPw();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}