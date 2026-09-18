# OWASP Top 10 보안 점검 및 처리 워크리스트

코드베이스(`src/`)를 OWASP Top 10 (2021) 기준으로 점검한 결과와, **하나씩 처리하기 위한 워크리스트**입니다.
각 항목은 `SEC-xx` 식별자를 가지며, 처리 순서는 §4의 우선순위(높음 → 낮음)를 따릅니다.
처리 완료 시 해당 항목의 체크박스를 `[x]`로 바꾸고 **처리 내역**에 커밋/날짜/방법을 기록한다.

- 기준: 현재 `src/` 코드 (Spring Boot 4.0.7 + JPA + SQLite, JWT 인증 REST API)
- 점검 범위: `security/`, `user/`, `common/`, `logging/`, `application*.yaml`, `build.gradle`
- 상태 범례: ⬜ 미처리 / 🔄 진행중 / ✅ 완료 / ⏸ 보류(의도적)

## 1. 종합 요약

| ID | OWASP 항목 | 심각도 | 상태 | 핵심 내용 |
| --- | --- | :---: | :---: | --- |
| SEC-01 | A07 인증 실패 | 🔴 높음 | 🔄 | 로그인 무차별 대입(브루트포스) 방지 없음 — 레이트리밋·계정 잠금 부재 |
| SEC-02 | A02/A05 암호화·설정 오류 | 🔴 높음 | ⬜ | JWT 시크릿 하드코딩 기본값 + 기본 프로파일 `dev` + prod/stg 프로파일 부재 |
| SEC-03 | A07 인증 실패 | 🟠 중간 | ⬜ | 사용자 열거 — 로그인 오류 메시지로 계정 존재 여부 노출 |
| SEC-04 | A09 로깅·모니터링 | 🟠 중간 | ⬜ | 보안 감사 로그 부재 — 로그인 실패·관리자 작업·비밀번호 변경 추적 없음 |
| SEC-05 | A07 인증 실패 | 🟠 중간 | ⬜ | 비밀번호 변경 시 기존 토큰 무효화 없음 — 변경 후에도 만료까지 유효 |
| SEC-06 | A06 취약·노출 컴포넌트 | 🟠 중간 | ⬜ | 의존성 취약점 스캐닝 미구성 + 미사용 `oauth2-client` 의존성 잔존 |
| SEC-07 | A08 소프트웨어·데이터 무결성 | 🟠 중간 | ⬜ | 스키마 마이그레이션 도구(Flyway/Liquibase) 부재 — `ddl-auto: update` 의존 |
| SEC-08 | A04 비보안 설계 | 🟡 낮음 | ⬜ | 매 요청 DB 조회(부하 증폭), MFA·비밀번호 히스토리 부재 |
| SEC-09 | A05 설정 오류 | 🟡 낮음 | ⬜ | HTTPS 강제·보안 헤더(CSP 등) 미설정 (대부분 인프라 레벨) |
| SEC-10 | A01 접근 제어 | 🟢 양호 | ⏸ | `/admin/**` → `ROLE_ADMIN`, 기본 `authenticated()`, 매 요청 DB 역할 재확인 |
| SEC-11 | A03 인젝션 | 🟢 양호 | ⏸ | JPA/JPQL 네임드 파라미터 100%, 네이티브 쿼리·문자열 연결 없음 |
| SEC-12 | A10 SSRF | 🟢 양호 | ⏸ | 외부 HTTP 요청·파일 업로드 없음 |

> 🟢 양호 항목(SEC-10~12)은 "문제 없음" 확인 기록이며, 별도 조치 없이 ⏸(의도적 보류)로 유지한다.

## 2. 상세 점검 항목

### SEC-01. 로그인 브루트포스 방지 부재 🔴 높음

- **OWASP**: A07:2021 – Identification and Authentication Failures
- **상태**: 🔄 진행 중
- **위치**: `src/main/java/com/template/core/user/controller/AuthController.java:29` → `src/main/java/com/template/core/user/service/UserService.java:122` (`login`)
- **문제**: `/api/auth/login`에 **레이트리밋·계정 잠금·지수 백오프·CAPTCHA가 전혀 없다.** 공격자가 무제한으로 비밀번호를 시도할 수 있다.
- **권장**: 로그인 엔드포인트에 IP/계정 단위 레이트리밋 적용 (예: `bucket4j` 또는 게이트웨이 레벨), 연속 실패 N회 시 잠금 또는 지연.
- **처리 내역**:
  - [ ] 2026-09-18 (7dd49ab) 계정(로그인 ID) 단위 슬라이딩 윈도우 레이트리밋 구현 — `LoginRateLimiter`(인메모리, `login.rate-limit.*` 설정: 5회/900초), 한도 도달 시 429 `TOO_MANY_ATTEMPTS`(`TooManyAttemptsException`), 성공 시 기록 초기화
  - [ ] (날짜) (커밋) (방법)

### SEC-02. JWT 시크릿 기본값 + dev 기본 프로파일 + prod 프로파일 부재 🔴 높음

- **OWASP**: A02:2021 – Cryptographic Failures / A05:2021 – Security Misconfiguration
- **상태**: ⬜ 미처리
- **위치**:
  - `src/main/resources/application-dev.yaml:19` → `secret: ${JWT_SECRET:dev-only-change-me-0123456789abcdef0123456789abcdef}`
  - `src/main/resources/application.yaml:7` → 기본 프로파일 `dev`
  - `application-prod.yaml` / `application-stg.yaml` **부재**
- **문제**:
  1. `JWT_SECRET` 환경변수를 설정하지 않으면 **누구나 아는 기본 시크릿**으로 토큰 서명 → 토큰 위조 가능.
  2. 프로파일을 명시하지 않고 배포하면 dev 설정으로 실행된다.
  3. 강화된 운영 설정(prod/stg)이 아예 없다.
- **권장**:
  - prod에서는 `JWT_SECRET`이 없으면 **시작을 실패**하게 강제 (fail-fast).
  - `application-prod.yaml` 신설: `ddl-auto: none`, `show-sql: false`, Swagger 비활성, 짧은 토큰 유효기간.
  - 기본 프로파일을 `dev`가 아닌 안전값으로 변경하거나, prod 배포 시 프로파일 미지정을 차단.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-03. 사용자 열거 (User Enumeration) 🟠 중간

- **OWASP**: A07:2021 – Identification and Authentication Failures
- **상태**: ⬜ 미처리
- **위치**: `src/main/java/com/template/core/user/service/UserService.java:125` / `:128` / `:132`
- **문제**: 로그인 오류 메시지가 계정 존재 여부를 구분한다.
  - `:125` → `"사용자를 찾을 수 없습니다. id=..."` (400)
  - `:128` → `"이미 탈퇴한 회원입니다."` (409)
  - `:132` → `"비밀번호가 일치하지 않습니다."` (400)
  - 공격자가 유효한 아이디 목록을 수집할 수 있다. `LoginRequest` Javadoc은 "포맷 정보 유출 방지"를 의도했지만 서비스 메시지로 여전히 유출된다.
- **권장**: 로그인 실패 시 **동일한 범용 메시지**("아이디 또는 비밀번호가 올바르지 않습니다.")로 통일.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-04. 보안 감사 로그 부재 🟠 중간

- **OWASP**: A09:2021 – Security Logging and Monitoring Failures
- **상태**: ⬜ 미처리
- **위치**: `src/main/java/com/template/core/common/error/GlobalExceptionHandler.java` (클라이언트 오류만 WARN), `UserService` (보안 이벤트 로그 없음)
- **문제**: **로그인 성공/실패, 관리자 코드 변경, 비밀번호 변경, 탈퇴** 등 보안 이벤트에 대한 감사 로그가 없다. 침해 탐지·사후 추적이 어렵다.
- **권장**: 인증 실패, 관리자 API 호출, 민감 상태 전환(탈퇴/비밀번호 변경)에 구조화된 감사 로그 추가.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-05. 비밀번호 변경 시 토큰 무효화 없음 🟠 중간

- **OWASP**: A07:2021 – Identification and Authentication Failures
- **상태**: ⬜ 미처리
- **위치**: `src/main/java/com/template/core/user/service/UserService.java:90` (`changePassword`), `src/main/java/com/template/core/security/JwtService.java:37` (`createToken`)
- **문제**: `changePassword` 후에도 기존에 발급된 JWT는 **만료(개발 24h)까지 유효**하다. `createToken`에 `jti`(토큰 ID)가 없어 블랙리스트/무효화 메커니즘이 없다.
- **권장**: `jti` 클레임 추가 + 토큰 버전/발급 시각 기준 무효화, 또는 리프레시 토큰 도입.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-06. 의존성 취약점 스캐닝 미구성 + 미사용 의존성 🟠 중간

- **OWASP**: A06:2021 – Vulnerable and Outdated Components
- **상태**: ⬜ 미처리
- **위치**: `build.gradle:24` (`spring-boot-starter-security-oauth2-client`), `build.gradle` 전체 (스캐닝 부재)
- **문제**:
  1. OWASP dependency-check / Dependabot 등 **취약점 스캐닝이 없다.**
  2. `spring-boot-starter-security-oauth2-client`가 **미사용 의존성**으로 잔존 (docs/ADDITIONAL_FEATURES.md §3-1에서도 지적). 미사용이라도 클래스패스에 로드되어 CVE 공격표면이 된다.
- **권장**: 의존성 스캐닝(CI) 도입, 미사용 의존성 제거.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-07. 스키마 마이그레이션 도구 부재 🟠 중간

- **OWASP**: A08:2021 – Software and Data Integrity Failures
- **상태**: ⬜ 미처리
- **위치**: `src/main/resources/application-dev.yaml:9` → `ddl-auto: update`
- **문제**: 버전 관리되는 마이그레이션(Flyway/Liquibase) 없이 엔티티 변경이 DB에 자동 반영된다. 운영에서 데이터 무결성·롤백 관점에서 위험하다.
- **권장**: Flyway/Liquibase 도입, prod는 `ddl-auto: none`.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-08. 매 요청 DB 조회 / MFA·비밀번호 히스토리 부재 🟡 낮음

- **OWASP**: A04:2021 – Insecure Design
- **상태**: ⬜ 미처리
- **위치**: `src/main/java/com/template/core/security/JwtAuthenticationFilter.java:46` (매 요청 `findByLoginId`), `UserService.java:103` (직전 비밀번호 1개만 대조)
- **문제**:
  1. **모든 요청마다** DB를 조회해 역할을 재확인한다. 최신성 확보에는 좋으나 고부하/DDoS 시 DB 부하가 증폭된다.
  2. MFA 없음. `changePassword`는 직전 비밀번호 1개만 대조해 재사용이 가능하다.
- **권장**: 역할 캐싱 또는 토큰 클레임 활용 검토. MFA·비밀번호 히스토리(최근 N개) 도입 검토.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

### SEC-09. HTTPS 강제·보안 헤더 미설정 🟡 낮음

- **OWASP**: A05:2021 – Security Misconfiguration
- **상태**: ⬜ 미처리
- **위치**: `src/main/java/com/template/core/security/SecurityConfig.java` (requireSSL/CSP 없음)
- **문제**: 코드 레벨에서 `requireSSL()`/CSP 등 보안 헤더 미설정. 보통 리버스 프록시/인프라에서 처리하므로 확인 필요.
- **권장**: 리버스 프록시/인프라 레벨에서 HTTPS 강제·보안 헤더(CSP, HSTS, X-Content-Type-Options 등) 적용 확인. 로드밸런서 뒤 HTTPS 감지를 위해 `server.forward-headers-strategy` 검토.
- **참고**: `SecurityConfig.java:45`의 CSRF 비활성은 쿠키 미사용의 무상태 JWT API이므로 **정상**이다.
- **처리 내역**:
  - [ ] (날짜) (커밋) (방법)

## 3. 양호한 부분 (조치 불필요 — 유지)

| ID | OWASP 항목 | 확인 내용 |
| --- | --- | --- |
| SEC-10 | A01 접근 제어 | `/admin/**` → `hasRole("ADMIN")`, 기본 `anyRequest().authenticated()`(기본 거부). 토큰과 무관하게 **매 요청 DB의 최신 역할/상태를 재확인**해 권한 상승·탈퇴 회원 차단이 정확. |
| SEC-11 | A03 인젝션 | 모든 DB 접근이 JPA/JPQL **네임드 파라미터**(`:loginId`, `:cutoff`). 네이티브 쿼리·문자열 연결 SQL 없음. 모든 요청 DTO에 Bean Validation(`@Valid`) 적용. |
| SEC-12 | A10 SSRF | 외부 HTTP 요청·파일 업로드가 없어 해당 위험 없음. |

추가로 잘 된 부분:
- **A02 암호화**: 비밀번호 **BCrypt** 저장, JWT **HS256** + 키 길이 검증(`JwtService.java:33`, 256bit 미만이면 시작 실패).
- **비밀번호 정책**: `StrongPasswordValidator` — 8~72자, 4종 중 3종 조합, 연속/반복 금지, 취약 단어 블랙리스트, ID 포함 금지.
- **정보 노출 제한**: 응답 DTO에 비밀번호·내부 필드 미노출, 500은 범용 메시지로 내부 정보 차단(`GlobalExceptionHandler.java:50`), Actuator는 `health,info`만 노출, Swagger는 dev 전용.
- **기밀 파일 관리**: `.gitignore`에 `*.db`, `.env` 포함, git에 민감 파일 미추적.

## 4. 처리 우선순위

| 순위 | ID | 항목 | 비고 |
| :---: | --- | --- | --- |
| 1 | SEC-01 | 로그인 레이트리밋/잠금 | 가장 현실적인 공격 벡터 |
| 2 | SEC-02 | JWT 시크릿 fail-fast + prod 프로파일 신설 | 기본값 시크릿으로 배포되는 것을 원천 차단 |
| 3 | SEC-03 | 로그인 오류 메시지 통일 | 소규모 변경으로 즉시 효과 (사용자 열거 차단) |
| 4 | SEC-04 | 보안 감사 로그 | 침해 탐지·사후 추적 기반 마련 |
| 5 | SEC-05 | 토큰 무효화(jti/리프레시) | 비밀번호 변경·탈퇴 시 세션 통제 |
| 6 | SEC-06 | 의존성 스캐닝 + 미사용 의존성 제거 | CVE 공격표면 축소 |
| 7 | SEC-07 | 스키마 마이그레이션 도구 | 운영 데이터 무결성 |
| 8 | SEC-08 | 부하 / MFA·히스토리 | 설계 개선 (검토) |
| 9 | SEC-09 | HTTPS·보안 헤더 | 인프라 레벨 확인 |

## 5. 처리 로그

> 항목을 하나씩 처리하면서 아래에 기록한다. (날짜 / ID / 커밋 / 요약)

| 날짜 | ID | 커밋 | 요약 |
| --- | --- | --- | --- |
| - | - | - | - |



