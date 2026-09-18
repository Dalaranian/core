# 코드 컨벤션 (Code Convention)

이 파일은 `.clinerules` 디렉토리에 있으므로 새 세션 시작 시 Cline이 자동으로 로드합니다.
새 코드를 작성할 때 아래 규칙을 따른다. 자세한 예시와 흐름은 `docs/DEVELOPMENT_GUIDE.md`를 참고한다.

## 1. Request DTO는 Bean Validation 필수

외부 입력을 받는 **요청(Request) DTO를 새로 만들면 Bean Validation을 반드시 적용한다.**
신뢰 경계(트러스트 바운더리)이므로 서비스 로직이 아니라 DTO에서 입력을 검증한다.
검증 실패 시 `GlobalExceptionHandler`가 400 + `VALIDATION_ERROR` + 필드별 오류(`fieldErrors`)로 응답한다.

### 적용 규칙

- 컨트롤러 파라미터에 `@Valid @RequestBody`를 붙여 검증이 실제로 실행되게 한다.
- 필수 문자열: `@NotBlank` + `@Size(max = 엔티티 컬럼 길이)` — `@Size`의 max는 엔티티 컬럼 길이와 일치시킨다.
- 숫자 범위: `@Min`/`@Max` 등.
- 교차 필드·비밀번호 정책 같은 복합 규칙은 클래스 레벨 커스텀 제약으로 처리한다 (`user/validation/StrongPassword` 참고).
- 검증 메시지는 한국어 인라인으로 쓴다.

### 예외 (검증 방식이 다른 경우)

- **부분 수정(PATCH) DTO**: "null = 변경 없음"이므로 `@NotBlank`를 쓰지 않고, 값이 있을 때만 유효한 규칙(`@Size`/`@Min` — null 통과)만 적용한다 (`CodeUpdateRequest` 참고).
- **포맷 유출 방지 요청(로그인 등)**: `@NotBlank`만 적용하고 크기·조합 규칙은 검사하지 않는다 (`LoginRequest` 참고).
- **DB 상태와 비교해야 하는 검증**(기존 비밀번호 대조, 직전 비밀번호 동일 확인 등)은 서비스에서 한다. 다만 **필수 여부 등 형식 검증은 여전히 DTO에서** 처리한다.
  - 참고: 기존 `ChangePasswordRequest`는 서비스 레벨 검증에만 의존하는 예외다. 신규 DTO에서는 이 방식을 쓰지 않는다.

### 예시

```java
public record UserJoinRequest(
        @NotBlank(message = "아이디는 필수입니다.") @Size(max = 50, message = "아이디는 50자 이하여야 합니다.") String id,
        @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.") String pw) {
}
```

## 2. 패키지 구조

- 도메인 중심 패키징: 도메인 폴더 아래 `controller/dto/entity/repository/service`.
- 도메인 전용 클래스(스케줄러, 설정)는 도메인 폴더 루트에 둔다 (예: `user/WithdrawalCleanupScheduler`).
- 공용 인프라(security, logging, common)는 도메인 폴더 밖에 둔다.

## 3. 컨트롤러

- 얇게 유지한다: 서비스 위임 + `ApiResponse` 래핑 외 로직을 넣지 않는다. 형식 검증은 Bean Validation, 상태 전환·예외 판단은 서비스가 담당한다.
- 모든 응답은 `ApiResponse<T>` 봉투로 감싼다 (인증 엔드포인트 포함 예외 없음). 본문 없는 성공은 `ApiResponse.success(null)`.
- 인증 주체는 `Authentication` 파라미터로 받고, 서비스에는 `authentication.getName()`만 넘긴다. `UserPrincipal`은 컨트롤러에서 다루지 않는다.
- 생성자 주입: `@RequiredArgsConstructor` + `private final`.
- 한국어 Javadoc: 메서드는 한 줄 요약, 복잡한 동작은 여러 줄.
- 경로는 `/api/<복수형 도메인>`. 상태 변경은 POST, 본인 리소스 삭제는 `@DeleteMapping("/me")`.

## 4. 서비스

- 쓰기: `@Transactional`, 조회 전용: `@Transactional(readOnly = true)`.
- 표준 예외를 던지고 `GlobalExceptionHandler`가 매핑하게 한다: `IllegalStateException` → 409 (`DUPLICATE_STATE`), `IllegalArgumentException` → 400 (`INVALID_ARGUMENT`), 그 외 → 500. 커스텀 예외 클래스를 만들기 전에 표준 예외로 충분한지 먼저 판단한다.
- 예외 메시지에는 식별 정보를 포함하되, 민감 정보(비밀번호 등)는 넣지 않는다.
- DTO/엔티티 경계를 책임진다: 요청 → 엔티티는 빌더로, 엔티티 → 응답은 DTO의 `from` 팩토리로.
- 민감 데이터는 인코딩 후 저장(`passwordEncoder.encode`), 비교는 `matches`로만.
- 도메인 상태 변경은 엔티티 메서드로 캡슐화한다 (예: `user.withdraw()`).
- 응답 노출 필드를 제한한다 (비밀번호·상태 코드 등 내부 필드 금지).

## 5. DTO

- 불변 record, 클래스당 하나의 목적(요청 or 응답).
- 파일명: `<기능>Request` / `<기능>Response`, 각 필드에 `@param` Javadoc.
- 응답 DTO는 엔티티에서 변환하는 정적 `from(entity)` 팩토리를 제공한다.

## 6. 엔티티 & 리포지토리

- Lombok: `@Getter` + `@NoArgsConstructor(PROTECTED)` + `@AllArgsConstructor(PRIVATE)` + `@Builder`.
- 상태(enum)는 코드값 컨버터(`@Convert`)로 저장, DB 기본값이 필요하면 `columnDefinition` 명시.
- 비즈니스 상태 전환 메서드는 엔티티 안에 둔다.
- 리포지토리: `JpaRepository` + `@Repository`. PK 외 컬럼 조회는 JPQL `@Query`로 명시(`findById` 충돌 방지), 벌크 삭제/수정은 `@Modifying` + JPQL.

## 7. 공통 코드 (CodeEnum)

- 코드성 enum은 `CodeEnum`을 구현하고 `CodeSyncRunner.SEED_CODES`에 등록한다. 누락하면 DB로 싱크되지 않는다.
- 비즈니스 코드 분기는 반드시 enum `==` 비교로 한다. DB 코드값 문자열 비교 분기는 만들지 않는다.
- 코드 조회는 `CodeService`(메모리 캐시)를 통해 한다. 코드 테이블을 직접 repository로 조회하지 않는다.

## 8. 테스트

- 서비스 단위 테스트는 Mockito로 의존성을 격리하고, 검증은 AssertJ로 한다.
- JUnit5 + `@DisplayName` 한국어, 메서드명은 영문 `동작_조건_결과` (예: `join_WithDuplicateLoginId_ThrowsException`).
- given/when/then 주석 구조를 유지한다.
- 엔티티 상태 전환·컨버터 등 도메인 로직은 별도 단위 테스트로 다룬다.

## 9. 공통 규칙

- 주석·Javadoc·로그 메시지는 **한국어**.
- 설정 주입은 `@ConfigurationProperties` 레코드(`JwtProperties`, `WithdrawalProperties` 참고)를 사용하고 `@Value` 개별 주입은 하지 않는다.
- API 응답/에러 형식을 바꿀 때는 `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler` 세 파일을 함께 점검한다.

