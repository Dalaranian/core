# Core

SSL 서비스용 **Spring 백엔드 보일러플레이트** 프로젝트입니다.

REST API 서버를 시작할 때마다 반복 작성하게 되는 공통 코드(트레이싱, 인증, 회원 골격, 공통 응답 등)를
미리 작성해 두어, **비즈니스 로직 작성에만 집중**할 수 있게 하는 것이 목적입니다.

## 제공되는 보일러플레이트

- **traceId 로깅**: 요청마다 UUID(`traceId`)를 MDC에 주입해 로그 추적 (콘솔/파일 롤링)
- **JWT 인증**: 로그인 시 HS256 토큰 발급, 필터에서 검증 (`JwtService` / `JwtAuthenticationFilter`)
- **회원 관리 골격**: 가입(`POST /api/users`), 로그인(`POST /api/auth/login`), 탈퇴(`DELETE /api/users/me`)
- **회원 탈퇴 유예**: 탈퇴 시 즉시 삭제하지 않고 유예 기간 후 매일 자정 배치로 제거 (`WithdrawalCleanupScheduler`)
- **공통 응답/에러**: `ApiResponse` 래퍼와 `GlobalExceptionHandler` 표준 에러 응답
- **SQLite 영속화**: JPA 기반, 별도 DB 설치 없이 바로 동작

## 기술 스택

- Java 21 / Spring Boot 4.0.7 / Gradle
- 데이터베이스: SQLite (JPA / Hibernate community dialect)
- 보안: Spring Security, JJWT 0.12.6
- API 문서: springdoc-openapi 3 (Swagger UI)
- 기타: Lombok, Actuator / 테스트: JUnit 5, AssertJ

## 프로젝트 구조

```
src/main/java/com/template/core/
├── CoreApplication.java              # Spring Boot 진입점
├── common/
│   ├── error/                        # GlobalExceptionHandler, ErrorResponse
│   └── response/                     # ApiResponse 공통 응답 래퍼
├── logging/TraceIdFilter.java        # 요청별 traceId(UUID) MDC 주입 필터
├── security/
│   ├── SecurityConfig.java           # 보안 필터 체인, PasswordEncoder
│   ├── JwtService.java               # JWT 발급/검증
│   ├── JwtProperties.java            # jwt.* 설정 바인딩
│   └── JwtAuthenticationFilter.java  # Authorization 헤더 토큰 인증 필터
└── user/
    ├── controller/                   # UserController(가입/탈퇴), AuthController(로그인)
    ├── service/                      # UserService, CustomUserDetailsService
    ├── principal/UserPrincipal.java  # UserDetails 어댑터
    ├── dto/                          # 가입/로그인/탈퇴 요청·응답 DTO
    ├── entity/                       # UserEntity, UserStatus
    ├── repository/UserRepository.java
    ├── WithdrawalCleanupScheduler.java  # 유예기간 경과 회원 삭제 배치(매일 자정)
    └── WithdrawalProperties.java     # user.withdrawal.* 설정 바인딩
src/main/resources/
├── application.yaml                  # 공통 설정 (프로파일, 로깅, 탈퇴 정책)
├── application-dev.yaml              # DEV 전용 (SQLite, JWT 설정)
└── logback-spring.xml                # 콘솔·파일 롤링 (10MB / 30일 / 1GB)
```

테스트는 `src/test/java/com/template/core/` 하위에 주요 컴포넌트별로 위치합니다
(traceId, JWT 필터, 전역 예외 처리, 회원 서비스, 탈퇴 배치, 컨텍스트 스모크).

## 시작하기

- 요구 사항: Java 21 이상

```bash
gradlew.bat run   # Windows (macOS/Linux: ./gradlew run)
gradlew.bat test
```

기본 프로파일은 `dev`(로컬 SQLite, 포트 `8080`)이며, 다른 프로파일은 환경변수로 덮어씁니다:
`SPRING_PROFILES_ACTIVE=stg gradlew.bat run`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 주요 설정

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `spring.profiles.active` | `dev` | 기본 프로파일 (배포 시 덮어씀) |
| `spring.datasource.url` | `jdbc:sqlite:./data/app.db` | SQLite 연결 (dev) |
| `spring.jpa.hibernate.ddl-auto` | `update` | 스키마 자동 반영 (dev) |
| `jwt.secret` | dev 기본값 | HS256 서명 키. **운영 시 `JWT_SECRET` 환경변수로 필수 덮어쓰기** (최소 32바이트) |
| `jwt.expiry-seconds` | `86400` | 토큰 유효기간(초) |
| `user.withdrawal.grace-days` | `1` | 탈퇴 유예 일수 (1 = 익일 자정 삭제) |
| `user.withdrawal.delete-cron` | `0 0 0 * * *` | 탈퇴 삭제 배치 실행 시각 |

> 로그 롤링 정책(용량·기간)은 `src/main/resources/logback-spring.xml`의 `LOG_PATH`(기본 `./logs`) 및 롤링 속성으로 조정할 수 있습니다.

## 액추에이터

Actuator 스타터가 포함되어 있으며, **익명 노출이 안전한 엔드포인트만 웹으로 노출**합니다.
JWT 인증 뒤에서도 접근 가능하도록 `SecurityConfig`의 `permitAll`에 등록되어 있습니다.

| 엔드포인트 | 설명 |
| --- | --- |
| `GET /actuator/health` | 애플리케이션·DB 등 컴포넌트 상태 (`show-details: always`). 로드밸런서/컴포즈 헬스체크용 |
| `GET /actuator/info` | 빌드 정보 등 (기본 비어 있음) |

설정 위치: `application.yaml`의 `management.*`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

`metrics`, `env`, `loggers` 등 민감 정보가 담기는 엔드포인트는 의도적으로 노출하지 않았습니다.
필요 시 `include`에 추가하고, 반드시 Spring Security 인증 뒤에서만 접근하도록 관리하세요
(`permitAll`에 등록하지 않으면 기본적으로 JWT 인증을 요구합니다).

## 보안 환경 변수

JWT secret 등 비밀 값은 환경 변수로 주입합니다(`JWT_SECRET` 등). 실제 비밀 값은 커밋하지 마십시오
(`.env*`, `.pem`, `.p12`, `.jks` 등은 `.gitignore`로 제외되어 있습니다).

## 새 프로젝트 적용 방법

1. 이 템플릿을 복사합니다.
2. `com.template.core` 패키지명과 `settings.gradle`의 프로젝트명을 실제 프로젝트에 맞게 변경합니다.
3. 이후에는 `Controller` → `Service` → `Repository`에 비즈니스 로직만 작성하면 됩니다.

## 관련 문서

문서는 `docs/` 폴더에서 관리한다.

- `docs/HELP.md` — 코드 생성 단계에서 만들어진 Gradle 기본 도움말
- `docs/ADDITIONAL_FEATURES.md` — 기획 대비 미구현 기능 분석 (TODO)
- `compose.yaml` — Docker Compose (현재 서비스 정의 없음)
