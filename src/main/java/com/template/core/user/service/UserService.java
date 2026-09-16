package com.template.core.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.security.JwtService;
import com.template.core.user.entity.UserEntity;
import com.template.core.user.code.UserStatus;
import com.template.core.user.repository.UserRepository;
import com.template.core.user.dto.LoginRequest;
import com.template.core.user.dto.LoginResponse;
import com.template.core.user.dto.UserJoinRequest;
import com.template.core.user.dto.UserJoinResponse;
import com.template.core.user.dto.WithdrawRequest;

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
     * 회원 탈퇴를 처리한다.
     *
     * <p>본인 확인을 위해 비밀번호를 대조한 뒤, 상태를 WITHDRAWN으로 변경하고
     * 탈퇴 신청 시각을 기록한다. 실제 데이터 삭제는 익일 자정 배치가 담당한다.</p>
     *
     * @param loginId 탈퇴 요청 사용자의 로그인 ID (인증 주체)
     * @param request 비밀번호 재확인 요청
     * @throws IllegalArgumentException 사용자가 없거나 비밀번호가 일치하지 않을 때
     */
    @Transactional
    public void withdraw(String loginId, WithdrawRequest request) {
        UserEntity user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. id=" + loginId));

        if (!passwordEncoder.matches(request.pw(), user.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        user.withdraw();
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
     * @throws IllegalStateException 탈퇴(WITHDRAWN) 상태 회원일 때
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByLoginId(request.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. id=" + request.id()));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다. id=" + request.id());
        }

        if (!passwordEncoder.matches(request.pw(), user.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return LoginResponse.of(jwtService.createToken(user), user);
    }
}