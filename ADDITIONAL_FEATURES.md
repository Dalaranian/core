# 기획 대비 추가 기능 분석 (TODO)

README(기획 문서)와 현재 구현(`src/`)을 비교해 분석한 문서입니다.
기준 커밋: `2579de46` (master)

## 1. 기획에 있으나 미구현/미완성인 기능

### 1-1. 공통 예외 처리 (`@RestControllerAdvice`) — 우선순위: 높음
- 현재 `UserService`가 던지는 `IllegalStateException`(ID 중복), `IllegalArgumentException`(사용자 없음/비밀번호 불일치)이
  별도 핸들러 없이 Spring 기본 500 에러로 노출됨.
- 이유(400/409)와 상태 코드가 구분되지 않고, 에러 응답 형식도 통일되지 않음.
- **추가**: `common/error/GlobalExceptionHandler` + 공통 에러 응답 DTO.
  기획 문서의 "공통 코드 미리 작성" 취지에 부합하는 가장 기본적인 보일러플레이트.

### 1-2. 요청 DTO 검증 (Bean Validation) — 우선순위: 높음
- `UserJoinRequest`, `LoginRequest`에 `@NotBlank` 등 검증 어노테이션이 전혀 없고,
  `spring-boot-starter-validation` 의존성도 없음.
- 빈 ID/비밀번호로 가입 요청이 그대로 DB에 저장될 수 있음(신뢰 경계 검증 누락).
- **추가**: validation 스타터 + DTO 검증 + `@Valid` 적용.

### 1-3. 회원 탈퇴 API — 우선순위: 높음
- `UserEntity.withdraw()` 메서드와 `UserStatus.WITHDRAWN`, 탈퇴 회원 토큰 거부 로직(`JwtAuthenticationFilter`)까지
  준비되어 있지만, **탈퇴 엔드포인트가 없음** (`UserService`에도 withdraw 메서드 없음).
- **추가**: `DELETE /api/users/me` (또는 `/api/users/withdraw`) + `UserService.withdraw`.

### 1-4. 인증 사용자 식별 (`@AuthenticationPrincipal` / `UserPrincipal`) — 우선순위: 중간
- `UserPrincipal`(UserDetails 어댑터)이 존재하지만 실제로 사용되지 않음.
  `JwtAuthenticationFilter`가 principal에 `String subject`를 넣고 있음.
- **추가**: 필터가 `UserPrincipal`을 principal로 설정하도록 정리하거나, 미사용 클래스는 제거.
- README 구조도에 있는 `UserPrincipal.java`가 "제공되는 보일러플레이트"라면 연동이 완성되어야 함.

### 1-5. OAuth2 Client — 우선순위: 낮음 (기획 확인 필요)
- `build.gradle`에 `spring-boot-starter-security-oauth2-client` 의존성과 테스트 스타터가 있으나
  설정(프로바이더 yaml)·컨트롤러·SecurityConfig 연동이 전혀 없음.
- README 기술 스택에 "OAuth2 Client"가 명시되어 있으므로, 소셜 로그인 골격을 구현하거나
  의존성/기술 스택 표기에서 제거할지 결정 필요.

### 1-6. STG/PROD 프로파일 — 우선순위: 중간
- README가 "SPRING_PROFILES_ACTIVE=stg gradlew.bat run" 예시를 제공하지만
  `application-stg.yaml` / `application-prod.yaml`이 없음.
- **추가**: 프로파일별 datasource/jwt 설정 파일(비밀값은 환경변수 주입).

### 1-7. Actuator 노출 설정 — 우선순위: 낮음
- actuator 스타터는 있으나 `management.endpoints` 설정이 없고,
  SecurityConfig `permitAll`에 `/actuator/health`가 포함되어 있지 않아
  무상태 JWT 환경에서 헬스체크가 401로 막힐 수 있음(배포 시 로드밸런서 헬스체크 장애 요인).

## 2. 기획 문서와 코드의 불일치 (문서 갱신 필요)

- README 프로젝트 구조에 `JwtAuthenticationFilter`, `JwtProperties`, `JwtService`,
  `JwtAuthenticationFilterTest`, `application-dev.yaml`의 jwt 설정 등이 일부 누락되어 있음 (현재 코드가 더 최신).
- `HELP.md`는 Spring Initializr 생성물 그대로이며 "compose.yaml에 서비스가 없어 실행 안 됨"이라는
  오래된 안내가 포함됨 — 현재는 SQLite 임베디드 실행이므로 내용 갱신 또는 정리 권장.
- `compose.yaml`이 빈 서비스 정의로 존재 — 실제 사용 계획이 없다면 제거 검토.

## 3. 권장 구현 순서

1. 공통 예외 처리 + 에러 응답 DTO (1-1)
2. Bean Validation 적용 (1-2)
3. 회원 탈퇴 API (1-3)
4. `UserPrincipal` 연동 정리 (1-4)
5. Actuator health permitAll + management 설정 (1-7)
6. STG/PROD 프로파일 (1-6)
7. OAuth2 방향 결정 후 구현 또는 제거 (1-5)
8. README/HELP 문서 갱신 (2)
