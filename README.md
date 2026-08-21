# Core

Spring Boot 기반 REST API 서버 템플릿입니다. HTTP 요청마다 고유한 **traceId(UUID)**를 부여하여 요청 단위로 로그를 추적할 수 있는 공통 기능을 포함한 프로젝트의 시작점 역할을 합니다.

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
- **SQLite 영속화**: JPA `ddl-auto: update` 설정으로 스키마를 자동 반영합니다.

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/template/core/
│   │   ├── CoreApplication.java          # Spring Boot 진입점
│   │   └── logging/
│   │       └── TraceIdFilter.java        # HTTP 요청별 traceId(UUID) MDC 주입 필터
│   └── resources/
│       ├── application.yaml              # Spring / JPA / 로깅 설정
│       └── logback-spring.xml            # 콘솔·파일 Appender 및 롤링 정책
└── test/java/com/template/core/
    ├── CoreApplicationTests.java         # 컨텍스트 로드 스모크 테스트
    └── logging/
        └── TraceIdFilterTests.java       # traceId 발급·정리 검증 테스트
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

애플리케이션은 기본 포트 `8080`에서 시작되며, Swagger UI는 다음 경로에서 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

### 테스트

```bash
gradlew test
```

- `TraceIdFilterTests`: 요청마다 서로 다른 UUID가 부여되고, 요청 종료 후 MDC에서 제거되는지 검증합니다.
- `CoreApplicationTests`: Spring 컨텍스트가 정상 로드되는지 확인하는 스모크 테스트입니다.

## 설정

주요 설정은 `src/main/resources/application.yaml`에 있습니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `spring.application.name` | `core` | 애플리케이션 이름 |
| `spring.datasource.url` | `jdbc:sqlite:./data/app.db` | SQLite DB 연결 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 스키마 자동 생성/반영 |
| `logging.pattern.console/file` | `%d ... traceId=%X{traceId:-} ...` | 로그 출력 패턴 (traceId 포함) |

> 로그 파일 롤링(용량·기간), 최대 이력 보관 정책은 `src/main/resources/logback-spring.xml`의 `LOG_PATH`(기본 `./logs`) 및 롤링 속성으로 조정할 수 있습니다.

## 환경 변수 (보안)

`.env` 또는 환경 변수에 보안 관련 설정을 주입하여 사용할 수 있습니다. 예시와 비밀 값 템플릿은 `.env.example`을 참고하고, 실제 비밀값은 커밋하지 마십시오. (`.env*`, `.pem`, `.p12`, `.jks` 등은 `.gitignore`로 제외되어 있습니다.)

## 관련 문서

- `HELP.md` — 코드 생성 단계에서 만들어진 Gradle 기본 도움말
- `compose.yaml` — Docker Compose (현재 서비스 정의 없음)