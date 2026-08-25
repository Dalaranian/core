package com.template.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
}