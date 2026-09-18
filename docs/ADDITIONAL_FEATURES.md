# 기획 대비 구현 현황 및 추가 작업

README(기획 문서)와 현재 구현(`src/`)을 비교해 정리한 개발 현황 문서입니다.
기준 커밋: `54c71da` (fix/auth-login-api-response, 2026-09-15) — **현재 코드 기준으로 작성됨.**

## 1. 구현 현황 요약

| 영역 | 상태 | 비고 |
| --- | --- | --- |
| 공통 예외 처리 | ✅ 완료 | `GlobalExceptionHandler` + `ErrorResponse`/`ApiResponse` 봉투 |
| 회원 가입/로그인 | ✅ 완료 | JWT 기반 (`JwtService`, `JwtAuthenticationFilter`) |
| 회원 탈퇴 | ✅ 완료 | 유예 배치(`WithdrawalCleanupScheduler`) + 탈퇴 회원 토큰 차단 포함 |
| 인증 사용자 식별 | ✅ 설계 확정 | `Authentication` 기반 (의도적 설계 — §3 참고) |
| Actuator | ✅ 완료 | health/info 노출 + `permitAll` |
| Bean Validation | ✅ 완료 | user/code 전체 요청 DTO + `@Valid` + 400 `VALIDATION_ERROR` 핸들러 (§2-1 참고) |
| STG/PROD 프로파일 | ❌ 미완료 | 환경별 설정 파일 필요 (P1) |
| OAuth2 Client | ⚠️ 기획 확인 필요 | 의존성만 존재, 실제 구현 없음 |
| HELP.md / compose.yaml | ✅ 정리 완료 | 모두 제거됨, docker-compose 의존성도 제거 |

## 2. 반드시 추가 구현할 기능

### 2-1. Bean Validation — ✅ 완료

user 도메인(`UserJoinRequest`, `LoginRequest`, `WithdrawRequest`)과 code 도메인
(`CodeGroupCreateRequest`, `CodeCreateRequest`, `CodeUpdateRequest`) 전체에 적용 완료.

구현 내역:

1. `build.gradle`에 `spring-boot-starter-validation` 추가 ✅
2. 요청 DTO 검증 어노테이션 적용 — `@Size(max=...)`는 엔티티 컬럼 길이와 일치 ✅
   - `UserJoinRequest`: `@NotBlank @Size` + 클래스 레벨 `@StrongPassword`
     (8~72자, 3종 이상 조합, 연속/반복 문자 차단, 취약 단어 블랙리스트, ID 포함 금지 — `user/validation/StrongPasswordValidator`)
   - `LoginRequest`/`WithdrawRequest`: `@NotBlank`만 (포맷 유출 방지, 정책은 가입 시에만)
   - `CodeUpdateRequest`: 부분 수정이므로 null 통과 규칙(`@Size`/`@Min`)만 적용
3. 컨트롤러 `@Valid` 적용: `UserController.join/withdraw`, `AuthController.login`, `CodeAdminController.createGroup/createCode/updateCode` ✅
4. `GlobalExceptionHandler`에 `MethodArgumentNotValidException` 핸들러 → 400 ✅
5. 응답 형식 — `ErrorResponse`에 `fieldErrors`(필드명 → 메시지) 추가, `@JsonInclude(NON_NULL)`로 기존 봉투 유지: ✅

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "요청 값이 올바르지 않습니다.",
    "fieldErrors": {
      "id": "아이디는 필수입니다.",
      "pw": "비밀번호는 8~72자여야 합니다."
    }
  },
  "traceId": "...",
  "timestamp": "..."
}
```

6. 메시지 정책: 어노테이션에 한국어 인라인 사용 — `messages.properties` 분리는 보류(규칙이 늘어날 때 도입) ✅

작업 범위(참고용 원본):

1. `build.gradle`에 `spring-boot-starter-validation` 추가
2. Request DTO에 검증 어노테이션 적용 (`@NotBlank`, 크기 제한 등)
   - 예: `UserJoinRequest.id` → `@NotBlank`, `pw` → `@NotBlank` + 최소 길이 정책
3. 컨트롤러 파라미터에 `@Valid` 적용 (`UserController.join`, `AuthController.login`, `UserController.withdraw`)
4. `GlobalExceptionHandler`에 `MethodArgumentNotValidException` 핸들러 추가 → 400 응답
5. 검증 실패 응답 형식 정의 — 기존 `ApiResponse` 봉투(`success`/`data`/`error`/`traceId`/`timestamp`)와 충돌하지 않도록
   `ErrorResponse`를 확장하거나 필드별 오류를 담는 구조로 통일
6. 필드별 오류 메시지 정책 결정 (메시지를 어노테이션에 인라인할지, `messages.properties`로 분리할지)

### 2-2. STG/PROD 프로파일 — P1

README가 `SPRING_PROFILES_ACTIVE=stg` 실행 예시를 제공하지만
`application-stg.yaml` / `application-prod.yaml`이 없다.

작업 범위:

1. `application-stg.yaml`, `application-prod.yaml` 추가 (datasource, ddl-auto 등 환경별 설정)
2. 민감정보는 파일에 직접 기록하지 않고 환경변수 주입 — `application-dev.yaml`의 기존 패턴을 따른다:

```yaml
jwt:
  secret: ${JWT_SECRET}
```

3. README의 "주요 설정" 표와 실제 프로파일 파일 내용 일치 확인

## 3. 기획 여부 확인이 필요한 기능

### 3-1. OAuth2 Client — ⚠️ 기획 확인 필요

`build.gradle`에 `spring-boot-starter-security-oauth2-client` 의존성(및 테스트 스타터)이 있으나
프로바이더 설정·컨트롤러·`SecurityConfig` 연동은 전혀 없다. README 기술 스택에는 "OAuth2 Client"가 명시되어 있다.

- **구현한다면**: 프로바이더 yaml 설정, 성공/실패 핸들러, JWT 발급 연동, `SecurityConfig` 필터 체인 조정, 테스트 스타터 정리
- **구현하지 않는다면**: `build.gradle`에서 oauth2-client 의존성 2건 제거 + README 기술 스택 표기 수정

### 3-2. `UserPrincipal` — 설계 확정 사항 (미구현 아님)

`UserPrincipal`은 `CustomUserDetailsService`에서 사용 중이며 미사용 클래스가 아니다.
컨트롤러가 `Authentication authentication` 파라미터 + `authentication.getName()`으로 로그인 ID를 받는 것은
개발 가이드(`docs/DEVELOPMENT_GUIDE.md` 규칙 4번)로 **확정된 의도적 설계**다.

- 현재 상태: `JwtAuthenticationFilter`가 principal에 `String subject`를 넣고, 컨트롤러는 `Authentication`으로만 식별
- 선택적 개선(P3): 역할/권한 도입 시점에 필터가 `UserPrincipal`을 principal로 설정하도록 확장 검토
  (`UserPrincipal.getAuthorities()`의 `TODO: 역할 도입 시 권한 매핑` 참고)

## 4. 운영/배포 환경 구성

### 4-1. Actuator — ✅ 완료

`application.yaml`의 `management.endpoints`가 health, info만 웹으로 노출하고,
`SecurityConfig` `permitAll`에 `/actuator/health`, `/actuator/info`가 등록되어 있다.
과거의 "헬스체크 401" 이슈는 해결됨.

선택적 개선(P3):

- k8s 사용 시 liveness/readiness probe 노출 검토
- ✅ `show-details`를 `when-authorized`로 조정 완료 — 익명 요청은 `status`만, 인증된 요청은 DB 등 컴포넌트 상세 노출

### 4-2. 민감정보 외부화 — 부분 완료

`jwt.secret`은 이미 `${JWT_SECRET:dev-only-...}` 패턴으로 환경변수 주입형이다.
STG/PROD 프로파일 작성 시(§2-2) 이 패턴을 유지하고 기본값을 두지 않는다(`${JWT_SECRET}`).

## 5. 문서 및 프로젝트 정리

- ✅ README가 최신 구조(`JwtAuthenticationFilter`, `JwtProperties`, 탈퇴 API, 액추에이터 섹션 등)로 갱신됨
- ✅ `HELP.md` 제거 — Spring Initializr 보일러플레이트이며 README가 문서 역할을 대신함
- ✅ `compose.yaml` 제거 — 빈 서비스 정의만 있어 실사용 계획 없음으로 판단.
  `build.gradle`의 `spring-boot-docker-compose` 의존성도 함께 제거됨

추가 정리 여지(P2): §3-1에서 OAuth2를 제거하기로 결정하면 README 기술 스택 표기도 함께 수정.

## 6. 권장 작업 순서

| 순위 | 작업 | 비고 |
| --- | --- | --- |
| ✅ | Bean Validation 적용 + `GlobalExceptionHandler`에 validation 예외 처리 (§2-1) | 입력값 신뢰 경계 검증 |
| P1 | STG/PROD 프로파일 + 민감정보 외부화 (§2-2) | 배포에 필요한 최소 운영 설정 |
| P2 | OAuth2 방향 결정 → 구현 또는 의존성·문서 제거 (§3-1) | 기획 확인 선행 |
| P3 | Actuator 세부 개선, `UserPrincipal` 권한 확장 등 선택적 리팩터링 (§3-2, §4-1) | 필요 시점에 |
