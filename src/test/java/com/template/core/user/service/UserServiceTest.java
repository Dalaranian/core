package com.template.core.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.template.core.security.JwtService;
import com.template.core.user.UserEntity;
import com.template.core.user.UserRepository;
import com.template.core.user.UserStatus;
import com.template.core.user.dto.LoginRequest;
import com.template.core.user.dto.LoginResponse;
import com.template.core.user.dto.UserJoinRequest;
import com.template.core.user.dto.UserJoinResponse;

/**
 * UserService 비즈니스 로직 단위 테스트.
 *
 * <p>리포지토리/암호화기/JWT 발급기를 Mockito로 격리해 외부 I/O 없이
 * 빠르고 반복 가능하게(F.I.R.S.T) 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("가입 시 비밀번호를 인코딩해 저장하고 가입 결과를 반환한다")
    void join_WithNewLoginId_EncodesPasswordAndSavesUser() {
        // given: 신규 로그인 ID(중복 아님)와 인코딩 결과를 준비한다
        when(userRepository.existsByLoginId("alice")).thenReturn(false);
        when(passwordEncoder.encode("plain-pw")).thenReturn("encoded-pw");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when: 가입을 실행한다
        UserJoinResponse response = userService.join(
                new UserJoinRequest("alice", "plain-pw", "앨리스"));

        // then: 저장된 엔티티에 평문이 아닌 인코딩된 비밀번호가 담겼는지 확인한다
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        assertThat(response.id()).isEqualTo("alice");
        assertThat(captor.getValue().getPw()).isEqualTo("encoded-pw");
        assertThat(captor.getValue().getPw()).isNotEqualTo("plain-pw");
    }

    @Test
    @DisplayName("이미 사용 중인 로그인 ID로 가입하면 예외가 발생한다")
    void join_WithDuplicateLoginId_ThrowsException() {
        // given: 로그인 ID가 이미 존재한다
        when(userRepository.existsByLoginId("bob")).thenReturn(true);

        // when & then: 가입 시 IllegalStateException이 발생한다
        assertThatThrownBy(() -> userService.join(new UserJoinRequest("bob", "pw-2", "밥2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용 중인 로그인 ID");
    }

    @Test
    @DisplayName("가입된 회원의 초기 상태는 활성화(ACTIVE, 코드 10)이다")
    void join_WithNewUser_SetsActiveStatus() {
        // given: 신규 로그인 ID를 준비한다
        when(userRepository.existsByLoginId("erin")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when: 가입을 실행한다
        userService.join(new UserJoinRequest("erin", "plain-pw", "에린"));

        // then: 저장된 엔티티의 상태가 ACTIVE(코드 10)인지 확인한다
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(captor.getValue().getStatus().getCode()).isEqualTo(10);
    }

    @Test
    @DisplayName("올바른 비밀번호로 로그인하면 JWT가 발급된다")
    void login_WithCorrectPassword_ReturnsJwtResponse() {
        // given: 가입된 활성 회원과 일치하는 비밀번호, 발급될 JWT를 준비한다
        UserEntity user = UserEntity.builder()
                .id("carol")
                .pw("encoded-pw")
                .userName("캐롤")
                .build();
        when(userRepository.findByLoginId("carol")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-pw", "encoded-pw")).thenReturn(true);
        when(jwtService.createToken(user)).thenReturn("header.payload.signature");

        // when: 로그인을 실행한다
        LoginResponse response = userService.login(new LoginRequest("carol", "plain-pw"));

        // then: 사용자 정보와 JWT가 담긴 응답을 반환한다
        assertThat(response.id()).isEqualTo("carol");
        assertThat(response.userName()).isEqualTo("캐롤");
        assertThat(response.accessToken()).isEqualTo("header.payload.signature");
    }

    @Test
    @DisplayName("틀린 비밀번호로 로그인하면 예외가 발생한다")
    void login_WithWrongPassword_ThrowsException() {
        // given: 가입된 회원의 비밀번호 대조가 실패한다
        UserEntity user = UserEntity.builder()
                .id("dave")
                .pw("encoded-pw")
                .userName("데이브")
                .build();
        when(userRepository.findByLoginId("dave")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pw", "encoded-pw")).thenReturn(false);

        // when & then: 로그인 시 IllegalArgumentException이 발생한다
        assertThatThrownBy(() -> userService.login(new LoginRequest("dave", "wrong-pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("존재하지 않는 로그인 ID로 로그인하면 예외가 발생한다")
    void login_WithUnknownLoginId_ThrowsException() {
        // given: 로그인 ID에 해당하는 회원이 없다
        when(userRepository.findByLoginId("ghost")).thenReturn(Optional.empty());

        // when & then: 로그인 시 IllegalArgumentException이 발생한다
        assertThatThrownBy(() -> userService.login(new LoginRequest("ghost", "pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
