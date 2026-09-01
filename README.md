# Core

SSL 서비스용 **Spring 백엔드 보일러플레이트** 프로젝트입니다.

REST API 서버를 만들 때마다 반복 작성하게 되는 인프라/공통 코드(traceId 로깅, 보안 설정, 회원 관리 골격 등)를
미리 작성해 두어, **실제 프로젝트를 시작할 때는 바로 비즈니스 로직 작성에만 집중**할 수 있도록 하는 것이 목적입니다.

## 제공되는 보일러플레이트

- **traceId 로깅**: HTTP 요청마다 고유 UUID(`traceId`)를 자동 부여해 요청 단위로 로그를 추적
- **회원 관리 골격**: 회원 가입 API와 BCrypt 비밀번호 해시 저장
- **보안 설정 골격**: Spring Security 필터 체인, `UserDetailsService` 인증 연동
- **SQLite 영속화**: JPA 기반 영속화로 별도 DB 설치 없이 바로 동작

## 기술 스택

- **언어/런타임**: Java 21
- **프레임워크**: Spring Boot 4.0.7
- **빌드 도구**: Gradle
- **데이터베이스**: SQLite (JPA / Hibernate 연동)
- **보안**: Spring Security, OAuth2 Client
- **API 문서**: springdoc-openapi 3 (Swagger UI)
- **기타**: Lombok, Actuator
- **테스트**: JUnit 5, AssertJ

## 주요 기능

- **traceId 로깅**: `TraceIdFilter`가 `OncePerRequestFilter`로 동작하여 HTTP 요청마다
  새 UUID(`traceId`)를 SLF4J MDC에 주입합니다. 응답 후에는 반드시 MDC에서 제거하여
  스레드 풀 재사용 시 요청 간 로그 오염을 방지합니다.
- **콘솔/파일 로깅**: `logback-spring.xml`에서 일자·용량별 롤링 정책(10MB / 30일 / 1GB 상한)에 따라
  로그 파일을 `./logs/application.log`에 기록하고, 로그 패턴에 traceId를 포함합니다.
- **보안/인증**: `SecurityConfig`가 BCrypt `PasswordEncoder`를 제공하고,
  `CustomUserDetailsService`가 로그인 ID로 사용자를 조회해 Spring Security 인증에 연결합니다.
- **회원 가입**: `POST /api/users` 회원 가입 API가 ID 중복 확인 후 BCrypt 해시로 비밀번호를 저장합니다.
- **SQLite 영속화**: JPA `ddl-auto: update` 설정으로 스키마를 자동 반영합니다.

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/template/core/
│   │   ├── CoreApplication.java          # Spring Boot 진입점
│   │   ├── logging/
│   │   │   └── TraceIdFilter.java        # HTTP 요청별 traceId(UUID) MDC 주입 필터
│   │   ├── security/
│   │   │   └── SecurityConfig.java       # 보안 필터 체인, PasswordEncoder
│   │   └── user/
│   │       ├── controller/
│   │       │   ├── UserController.java   # 회원 관리 REST 엔드포인트
│   │       │   └── AuthController.java   # 로그인(JWT 발급) REST 엔드포인트
│   │       ├── service/
│   │       │   ├── UserService.java      # 회원 가입·로그인 비즈니스 로직
│   │       │   └── CustomUserDetailsService.java # 인증용 UserDetails 조회
│   │       ├── principal/
│   │       │   └── UserPrincipal.java    # UserDetails 어댑터
│   │       ├── dto/
│   │       │   ├── LoginRequest.java     # 로그인 요청 DTO
│   │       │   ├── LoginResponse.java    # 로그인(JWT) 응답 DTO
│   │       │   ├── UserJoinRequest.java  # 가입 요청 DTO
│   │       │   └── UserJoinResponse.java # 가입 응답 DTO
│   │       ├── UserEntity.java           # 사용자 엔티티 (SQLite)
│   │       ├── UserRepository.java       # JPA 리포지토리
│   │       └── UserStatus.java           # 회원 상태 enum·코드 변환기
│   └── resources/
│       ├── application.yaml              # Spring 공통 / 로깅 설정 (DB 접속 제외)
│       ├── application-dev.yaml          # DEV 전용 설정 (로컬 SQLite)
│       └── logback-spring.xml            # 콘솔·파일 Appender 및 롤링 정책
└── test/java/com/template/core/
    ├── CoreApplicationTests.java         # 컨텍스트 로드 스모크 테스트
    ├── logging/
    │   └── TraceIdFilterTests.java       # traceId 발급·정리 검증 테스트
    └── user/
        ├── UserEntityTest.java           # 엔티티 검증 테스트
        └── service/
            └── UserServiceTest.java      # 회원 가입·로그인 비즈니스 로직 테스트
```

## 시작하기

### 요구 사항

- Java 21 이상
- Gradle (또는 포함된 Gradle Wrapper 사용)

### 실행

프로젝트 루트에서 아래 명령을 실행합니다.

```bash
gradlew.bat run          # Windows
# 또는
./gradlew run            # macOS / Linux
```

기본 실행 프로파일은 `dev`(`application-dev.yaml`)이며, 로컬 SQLite로 동작합니다.
STG/PROD 등 다른 프로파일로 실행하려면 환경변수로 덮어쓰면 됩니다.

```bash
SPRING_PROFILES_ACTIVE=stg gradlew.bat run
```

애플리케이션은 기본 포트 `8080`에서 시작되며, Swagger UI는 다음 경로에서 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

### 테스트

```bash
gradlew test
```

- `TraceIdFilterTests`: 요청마다 서로 다른 UUID가 부여되고, 요청 종료 후 MDC에서 제거되는지 검증합니다.
- `UserServiceTest`: 회원 가입 시 중복 ID 검출, BCrypt 비밀번호 해시 저장 여부를 검증합니다.
- `CoreApplicationTests`: Spring 컨텍스트가 정상 로드되는지 확인하는 스모크 테스트입니다.

## 설정

환경 공통 설정은 `src/main/resources/application.yaml`, 개발(DEV) 전용 설정은
`src/main/resources/application-dev.yaml`에 있습니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `spring.application.name` | `core` | 애플리케이션 이름 |
| `spring.profiles.active` | `dev` | 기본 실행 프로파일 (배포 시 덮어써서 변경) |
| `spring.datasource.url` | `jdbc:sqlite:./data/app.db` | SQLite DB 연결 (**dev 프로파일**) |
| `spring.jpa.hibernate.ddl-auto` | `update` | 스키마 자동 생성/반영 (**dev 프로파일**) |
| `logging.pattern.console/file` | `%d ... traceId=%X{traceId:-} ...` | 로그 출력 패턴 (traceId 포함) |

> 로그 파일 롤링(용량·기간), 최대 이력 보관 정책은 `src/main/resources/logback-spring.xml`의 `LOG_PATH`(기본 `./logs`) 및 롤링 속성으로 조정할 수 있습니다.

## 환경 변수 (보안)

`.env` 또는 환경 변수에 보안 관련 설정을 주입하여 사용할 수 있습니다. 예시와 비밀 값 템플릿은 `.env.example`을 참고하고, 실제 비밀값은 커밋하지 마십시오. (`.env*`, `.pem`, `.p12`, `.jks` 등은 `.gitignore`로 제외되어 있습니다.)

## 새 프로젝트 적용 방법

1. 이 템플릿을 복사합니다.
2. `com.template.core` 패키지명, `settings.gradle`의 프로젝트명을 실제 프로젝트에 맞게 변경합니다.
3. `UserEntity` 등 도메인 엔티티를 확장하거나 새 도메인을 추가합니다.
4. 이후에는 `Controller` → `Service` → `Repository` 에 비즈니스 로직만 작성하면 됩니다.

## 관련 문서

- `HELP.md` — 코드 생성 단계에서 만들어진 Gradle 기본 도움말
- `compose.yaml` — Docker Compose (현재 서비스 정의 없음)