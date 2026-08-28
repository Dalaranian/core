# 커밋 컨벤션 (Commit Convention)

기존 커밋 히스토리를 분석해 정리한 규칙이다. 새 세션에서 커밋을 작성할 때 이 규칙을 따른다.

## 1. 제목 (Subject)

- 형식: `<type>: <한국어 요약>` (예: `feat: 로그인 API와 JWT 발급/검증 인증 추가`)
- type은 Conventional Commits의 소문자 영어로, 요약은 한국어로 작성한다.
  - `feat`: 기능 추가 (API, 인증, 엔티티 필드 등)
  - `fix`: 버그 수정
  - `refactor`: 동작 변경 없는 구조/구현 개선 (의존성 교체, 주입 방식 전환 등)
  - `chore`: 빌드·설정·문서·정리 (.gitignore, .clinerules, VS Code 설정 등)
  - 필요 시 `docs`(문서 전용), `test`(테스트 전용)도 허용
- 제목 끝에 마침표를 붙이지 않는다.
- 과거의 `init commit`, `update gitignore` 같은 type 없는 커밋은 초기 임시 커밋으로 간주하며 따르지 않는다.

## 2. 본문 (Body)

- 변경 사항을 주요 관심사별로 `-` 불릿으로 나열한다.
- 대상 컴포넌트를 접두사로 밝힌다: `- JwtService: @Value 개별 주입 제거 ...`
- "무엇을"과 함께 필요한 경우 "왜"(의도·배경)를 간결히 쓴다.
- 한 커밋은 한 관심사만 다룬다. 성격이 다른 변경(예: 설정 주입 리팩터링 + 신규 기능)은 커밋을 분리한다.

## 3. 예시

```
refactor: JWT 설정 주입을 @ConfigurationProperties 레코드로 전환

- JwtProperties 추가: application.yaml의 jwt.*(secret/expiry-seconds/issuer)를 타입 세이프로 바인딩
- JwtService: @Value 개별 주입을 제거하고 JwtProperties 단일 주입으로 단순화
- CoreApplication: @ConfigurationPropertiesScan 활성화
```
