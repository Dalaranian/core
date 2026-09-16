# 유저 엔티티 라이프사이클

`UserEntity`의 상태 전환과 데이터 보존/삭제 흐름, 역할(Role) 관리를 정리한 문서입니다.

## 상태 전환 흐름

```
가입 (POST /api/users)
   │  status = ACTIVE (코드 10)
   ▼
활성 회원 ──로그인 성공──▶ JWT 발급 (AuthService)
   │
   │  탈퇴 (DELETE /api/users/me, 비밀번호 재확인)
   ▼
탈퇴 신청 ──▶ status = WITHDRAWN (코드 20), withdrawnAt 기록
   │          ※ 데이터는 즉시 삭제되지 않음
   ▼
WithdrawalCleanupScheduler (매일 자정, user.withdrawal.delete-cron)
   │  withdrawnAt < cutoff 인 WITHDRAWN 회원 물리 삭제
   ▼
삭제 완료
```

## 단계별 상세

### 1. 가입 — ACTIVE

- `UserService.join`에서 `UserEntity.builder()`로 생성, 기본 상태는 `ACTIVE`(코드 10).
- DB 컬럼 기본값 `default 10`은 status 컬럼 추가 이전 기존 데이터를 활성화 회원으로 채우기 위한 것.

### 2. 탈퇴 신청 — WITHDRAWN (논리 삭제)

- `UserController.withdraw` → `UserService.withdraw(loginId, request)`.
- 비밀번호 대조(`passwordEncoder.matches`)로 본인 확인 후 `user.withdraw()` 호출.
- `UserEntity.withdraw()`가 상태를 `WITHDRAWN`(코드 20)으로 바꾸고 `withdrawnAt`에 현재 시각 기록.
- 이 시점까지는 **논리 삭제** 상태. 탈퇴 회원은 로그인 시 `IllegalStateException`(409)으로 차단되지만 데이터는 남아 있다.

### 3. 데이터 삭제 — 물리 삭제 (배치)

- `WithdrawalCleanupScheduler`가 매일 자정(`user.withdrawal.delete-cron`, 기본 `0 0 0 * * *`)에 실행.
- 유예 기간(`user.withdrawal.grace-days`, 기본 1일)이 지난, 즉 cutoff 이전에 탈퇴 신청한 `WITHDRAWN` 회원을 `UserRepository.deleteWithdrawnBefore` JPQL로 일괄 물리 삭제.
- cutoff 계산: `LocalDate.now().minusDays(graceDays - 1).atStartOfDay()` — 유예 1일이면 "어제 탈퇴 신청분"이 오늘 자정(익일)에 삭제됨.

## 역할(Role) 관리

사용자의 권한은 `UserRole` enum으로 관리하며, `/admin/**` 경로 접근 제어에 사용된다.

### UserRole enum

- `ROLE_USER(10)` — 일반 사용자 (신규 가입 기본값)
- `ROLE_ADMIN(20)` — 관리자
- `UserStatus`와 동일한 코드값 + `AttributeConverter`(`CodeConverter`) 패턴. DB에는 코드값(10/20)으로 저장되므로 역할 추가 시 enum에 코드만 이어서 부여하면 된다.
- `getAuthority()`는 enum 이름 그대로(`ROLE_USER`/`ROLE_ADMIN`)를 반환해 Spring Security 권한 문자열로 매핑된다.

### 권한 부여 흐름

```
로그인 성공 ──▶ JWT 발급 (subject = 로그인 ID, 역할 클레임 없음)
    │
    ▼
요청 시 JwtAuthenticationFilter
    │  1. 토큰 서명/만료 검증 → subject 추출
    │  2. DB에서 사용자 조회 (상태 ACTIVE만 통과, default deny)
    │  3. user.getRole().getAuthority() 로 권한 부여
    ▼
SecurityFilterChain 규칙 판단
    │  /admin/**  → hasRole("ADMIN") (ROLE_ADMIN 필요)
    │  그 외      → authenticated
```

- 역할을 JWT 클레임에 넣지 않고 **매 요청 DB에서 조회**하는 이유: 역할 변경이 기존 토큰 만료 전에도 즉시 반영된다. (조회 비용이 부담되면 토큰 클레임 방식 전환 검토)
- `UserPrincipal`(로그인 인증 경로)과 `JwtAuthenticationFilter`(토큰 인증 경로) 모두 `UserRole` 기반으로 권한을 부여한다. 이전에는 필터가 `ROLE_USER`를 하드코딩해 관리자도 `/admin/**`에서 403을 받는 버그가 있었다.

## 설정값 (application.yaml `user.withdrawal.*`)

`WithdrawalProperties`(@ConfigurationProperties 레코드)가 바인딩하며, 잘못된 값은 기본값으로 방어한다.

| 키 | 기본값 | 의미 |
|---|---|---|
| `grace-days` | `1` | 탈퇴 신청 후 삭제까지의 유예 일수 |
| `delete-cron` | `0 0 0 * * *` | 삭제 배치 실행 cron |

## 관련 코드

- `user/entity/UserEntity.java` — `withdraw()`, `UserStatus` 컨버터, `role` 필드
- `user/code/UserRole.java` — 역할 enum (`ROLE_USER`/`ROLE_ADMIN` + 코드 컨버터)
- `user/service/UserService.java` — 탈퇴·로그인 차단 로직
- `security/JwtAuthenticationFilter.java` — 토큰 검증 및 역할 기반 권한 부여
- `security/SecurityConfig.java` — `/admin/**` 접근 제어 규칙
- `user/WithdrawalCleanupScheduler.java` / `user/WithdrawalProperties.java` — 삭제 배치
- `user/repository/UserRepository.java#deleteWithdrawnBefore` — 벌크 삭제 쿼리
