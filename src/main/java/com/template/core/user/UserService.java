package com.template.core.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.security.JwtService;
import com.template.core.user.dto.LoginRequest;
import com.template.core.user.dto.LoginResponse;
import com.template.core.user.dto.UserJoinRequest;
import com.template.core.user.dto.UserJoinResponse;

import lombok.RequiredArgsConstructor;

/**
 * 회원 가입, 조회 등 사용자 관련 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * 회원 가입을 처리한다.
     *
     * <p>로그인 ID 중복을 먼저 확인하고, 비밀번호는 BCrypt로 인코딩해 평문으로 저장하지 않는다.</p>
     *
     * @param request 가입 요청 정보
     * @return 가입 완료된 사용자 응답
     * @throws IllegalStateException 이미 사용 중인 로그인 ID일 때
     */
    @Transactional
    public UserJoinResponse join(UserJoinRequest request) {
        if (userRepository.existsByLoginId(request.id())) {
            throw new IllegalStateException("이미 사용 중인 로그인 ID입니다. id=" + request.id());
        }

        UserEntity user = userRepository.save(UserEntity.builder()
                .id(request.id())
                .pw(passwordEncoder.encode(request.pw()))
                .userName(request.userName())
                .build());

        return UserJoinResponse.from(user);
    }

    /**
     * 로그인을 처리하고 JWT를 발급한다.
     *
     * <p>로그인 ID로 사용자를 찾아 BCrypt로 비밀번호를 대조하고, 성공 시
     * 해당 사용자에게 서명된 JWT를 발급해 반환한다.</p>
     *
     * @param request 로그인 요청 정보
     * @return 발급된 accessToken을 포함한 로그인 응답
     * @throws IllegalArgumentException 사용자가 없거나 비밀번호가 일치하지 않을 때
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByLoginId(request.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. id=" + request.id()));

        if (!passwordEncoder.matches(request.pw(), user.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return LoginResponse.of(jwtService.createToken(user), user);
    }
}