package com.template.core.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.security.JwtService;
import com.template.core.security.LoginRateLimiter;
import com.template.core.user.entity.UserEntity;
import com.template.core.user.code.UserStatus;
import com.template.core.user.repository.UserRepository;
import com.template.core.user.dto.ChangePasswordRequest;
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
    private final LoginRateLimiter loginRateLimiter;

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
     * 비밀번호를 변경한다.
     *
     * <p>기존 비밀번호 대조로 본인을 확인한 뒤, 새 비밀번호가 직전 비밀번호와
     * 동일하면 거부하고, 아니면 BCrypt로 인코딩해 저장한다.</p>
     *
     * @param loginId 변경 요청 사용자의 로그인 ID (인증 주체)
     * @param request 기존/새 비밀번호 요청
     * @throws IllegalArgumentException 사용자가 없거나 기존 비밀번호가 일치하지 않거나 새 비밀번호가 직전 비밀번호와 동일할 때
     * @throws IllegalStateException 탈퇴(WITHDRAWN) 상태 회원일 때
     */
    @Transactional
    public void changePassword(String loginId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. id=" + loginId));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다. id=" + loginId);
        }

        if (!passwordEncoder.matches(request.oldPw(), user.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(request.newPw(), user.getPw())) {
            throw new IllegalArgumentException("새 비밀번호가 직전 비밀번호와 동일합니다.");
        }

        user.changePassword(passwordEncoder.encode(request.newPw()));
    }

    /**
     * 로그인을 처리하고 JWT를 발급한다.
     *
     * <p>로그인 ID로 사용자를 찾아 BCrypt로 비밀번호를 대조하고, 성공 시
     * 해당 사용자에게 서명된 JWT를 발급해 반환한다.</p>
     *
     * <p>브루트포스 방지를 위해 슬라이딩 윈도우 내 실패 한도에 도달한 계정은
     * 로그인 자체를 거부(429)하고, 실패 시도는 기록하며, 성공 시 기록을 초기화한다.</p>
     *
     * @param request 로그인 요청 정보
     * @return 발급된 accessToken을 포함한 로그인 응답
     * @throws TooManyAttemptsException 윈도우 내 로그인 실패 횟수가 한도에 도달한 경우
     * @throws IllegalArgumentException 사용자가 없거나 비밀번호가 일치하지 않을 때
     * @throws IllegalStateException 탈퇴(WITHDRAWN) 상태 회원일 때
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 잠금 상태(윈도우 내 실패 한도 도달) 계정은 인증 로직 전에 거부한다.
        // 거부된 시도는 실패 기록에 추가하지 않아 잠금 기간이 무한 연장되지 않는다.
        loginRateLimiter.check(request.id());
        try {
            UserEntity user = userRepository.findByLoginId(request.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "사용자를 찾을 수 없습니다. id=" + request.id()));

            if (user.getStatus() == UserStatus.WITHDRAWN) {
                throw new IllegalStateException("이미 탈퇴한 회원입니다. id=" + request.id());
            }

            if (!passwordEncoder.matches(request.pw(), user.getPw())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }

            loginRateLimiter.recordSuccess(request.id());
            return LoginResponse.from(jwtService.createToken(user), user);
        } catch (RuntimeException e) {
            // 로그인 실패(존재하지 않는 ID/탈퇴 회원/비밀번호 불일치)를 모두 기록한다
            loginRateLimiter.recordFailure(request.id());
            throw e;
        }
    }
}