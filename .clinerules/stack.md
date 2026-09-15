# 기술 스택 (Tech Stack)

이 파일은 `.clinerules` 디렉토리에 있으므로 새 세션 시작 시 Cline이 자동으로 로드합니다. 매번 코드를 스캔하지 않고 아래 전제를 기본값으로 사용하세요.

## 스택 요약

- 백엔드: Java + SpringBoot, Gradle(`build.gradle`, `settings.gradle`) 기반 빌드
- 데이터 접근: JPA (Java Persistence API)
- DB: SQLite (`test.db` 포함)

## 주의 (스캔 없이 전제만으로 작업할 때)

- 위 정보는 전제일 뿐입니다. 작업 전 **필요한 부분은 실제 소스를 확인**하세요. 스택이 바뀌면 이 파일과 함께 갱신해주세요.
- 소스는 `src/` 아래, 빌드 설정은 루트의 `build.gradle` / `settings.gradle`에 있습니다.