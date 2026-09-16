package com.template.core.user.entity;

import com.template.core.user.code.UserRole;
import com.template.core.user.code.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 사용자 엔티티.
 *
 * <p>순번(PK)은 SQLite의 AUTOINCREMENT(시퀀스) 기능을 활용한다.
 * SQLite에는 별도의 시퀀스 객체가 없으므로 JPA {@link GenerationType#IDENTITY}로
 * 기본 키 컬럼에 AUTOINCREMENT를 부여하는 방식을 사용한다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "users")
public class UserEntity {

    /** 순번(PK). SQLite AUTOINCREMENT 기반으로 자동 증가 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq", nullable = false)
    private Long seq;

    /** 로그인 ID */
    @Column(name = "id", nullable = false, unique = true, length = 50)
    private String id;

    /** 로그인 PW */
    @Column(name = "pw", nullable = false, length = 200)
    private String pw;

    /** 사용자 이름 */
    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    /**
     * 회원 상태. 신규 가입 시 활성화(ACTIVE)가 기본값.
     * DB에는 코드값(10/20)으로 저장되며, columnDefinition의 default 10은
     * status 컬럼 추가 이전의 기존 데이터를 활성화 회원으로 채우기 위한 것이다.
     */
    @Builder.Default
    @Convert(converter = UserStatus.CodeConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "integer default 10")
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 사용자 역할. 신규 가입 시 일반 사용자(ROLE_USER)가 기본값.
     * DB에는 코드값(10/20)으로 저장되며, columnDefinition의 default 10은
     * role 컬럼 추가 이전의 기존 데이터를 일반 사용자로 채우기 위한 것이다.
     */
    @Builder.Default
    @Convert(converter = UserRole.CodeConverter.class)
    @Column(name = "role", nullable = false, columnDefinition = "integer default 10")
    private UserRole role = UserRole.ROLE_USER;

    /** 탈퇴 신청 시각. 탈퇴 배치가 익일 자정에 이 시각 기준으로 데이터를 삭제한다. */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    /** 회원을 탈퇴 상태(WITHDRAWN)로 변경하고 탈퇴 신청 시각을 기록한다. */
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }
}