package com.template.core.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
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

    @Builder
    public UserEntity(String id, String pw, String userName) {
        this.id = id;
        this.pw = pw;
        this.userName = userName;
    }
}