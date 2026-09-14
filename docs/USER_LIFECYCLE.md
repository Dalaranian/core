# 유저 엔티티 라이프사이클

`UserEntity`의 상태 전환과 데이터 보존/삭제 흐름을 정리한 문서입니다.

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

## 설정값 (application.yaml `user.withdrawal.*`)

`WithdrawalProperties`(@ConfigurationProperties 레코드)가 바인딩하며, 잘못된 값은 기본값으로 방어한다.

| 키 | 기본값 | 의미 |
|---|---|---|
| `grace-days` | `1` | 탈퇴 신청 후 삭제까지의 유예 일수 |
| `delete-cron` | `0 0 0 * * *` | 삭제 배치 실행 cron |

## 관련 코드

- `user/entity/UserEntity.java` — `withdraw()`, `UserStatus` 컨버터
- `user/service/UserService.java` — 탈퇴·로그인 차단 로직
- `user/WithdrawalCleanupScheduler.java` / `user/WithdrawalProperties.java` — 삭제 배치
- `user/repository/UserRepository.java#deleteWithdrawnBefore` — 벌크 삭제 쿼리
