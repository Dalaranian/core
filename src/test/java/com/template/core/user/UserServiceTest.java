package com.template.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.user.dto.LoginRequest;
import com.template.core.user.dto.LoginResponse;
import com.template.core.user.dto.UserJoinRequest;
import com.template.core.user.dto.UserJoinResponse;

/**
 * UserService.join() 핵심 로직(중복 ID 확인 + BCrypt 인코딩 저장) 검증 테스트.
 *
 * <p>실제 DB(SQLite)를 사용하고 트랜잭션 롤백으로 데이터가 남지 않게 한다.</p>
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 가입하면_비밀번호가_평문이_아닌_BCrypt로_저장된다() {
        UserJoinResponse response = userService.join(
                new UserJoinRequest("alice", "plain-pw", "앨리스"));

        assertThat(response.id()).isEqualTo("alice");

        UserEntity saved = userRepository.findByLoginId("alice").orElseThrow();
        assertThat(saved.getPw()).isNotEqualTo("plain-pw"); // 평문이 아님
        assertThat(passwordEncoder.matches("plain-pw", saved.getPw())).isTrue();
    }

    @Test
    void 중복_로그인ID로_가입하면_예외가_발생한다() {
        userService.join(new UserJoinRequest("bob", "pw-1", "밥"));

        assertThatThrownBy(() -> userService.join(new UserJoinRequest("bob", "pw-2", "밥2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용 중인 로그인 ID");
    }

    @Test
    void 올바른_비밀번호로_로그인하면_JWT가_발급된다() {
        userService.join(new UserJoinRequest("carol", "plain-pw", "캐롤"));

        LoginResponse response = userService.login(new LoginRequest("carol", "plain-pw"));

        assertThat(response.id()).isEqualTo("carol");
        assertThat(response.userName()).isEqualTo("캐롤");
        // JWT는 헤더.페이로드.서명 3부분으로 구성된다.
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.accessToken().split("\\.")).hasSize(3);
    }

    @Test
    void 틀린_비밀번호로_로그인하면_예외가_발생한다() {
        userService.join(new UserJoinRequest("dave", "correct-pw", "데이브"));

        assertThatThrownBy(() -> userService.login(new LoginRequest("dave", "wrong-pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    void 없는_로그인ID로_로그인하면_예외가_발생한다() {
        assertThatThrownBy(() -> userService.login(new LoginRequest("ghost", "pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    void 가입하면_회원상태가_활성화_코드10으로_저장된다() {
        userService.join(new UserJoinRequest("erin", "plain-pw", "에린"));

        UserEntity saved = userRepository.findByLoginId("erin").orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getStatus().getCode()).isEqualTo(10);
    }
}