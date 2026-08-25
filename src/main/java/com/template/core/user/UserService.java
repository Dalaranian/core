package com.template.core.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}