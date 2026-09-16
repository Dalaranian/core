# 개발 가이드 (Controller & 비즈니스 로직)

`com.template.core` 코드베이스의 실제 관례를 정리한 문서입니다.
새로운 도메인 기능(컨트롤러 + 서비스 + DTO + 엔티티 + 리포지토리)을 추가할 때 이 가이드를 따릅니다.

## 1. 패키지 구조

도메인 중심 패키징. 기능 단위로 폴더를 만들고 그 안에 레이어별 클래스를 둔다.

```
com.template.core
├── common
│   ├── code         # 공통 코드 (CodeEnum, CodeSyncRunner + entity/repository/service/controller/dto)
│   ├── error        # ErrorResponse, GlobalExceptionHandler
│   └── response     # ApiResponse (공통 응답 봉투)
├── logging          # TraceIdFilter
├── security         # JwtService, JwtProperties, JwtAuthenticationFilter, SecurityConfig
└── user             # 도메인 폴더 예시
    ├── controller   # AuthController, UserController
    ├── dto          # 요청/응답 record DTO
    ├── entity       # UserEntity
    ├── code         # UserRole, UserStatus 등 코드성 enum
    ├── repository   # UserRepository
    ├── principal    # UserPrincipal
    └── service      # UserService, CustomUserDetailsService
```

- 도메인 전용 클래스(스케줄러, 설정)는 도메인 폴더 루트에 둔다 (예: `user/WithdrawalCleanupScheduler`).
- 공용 인프라(security, logging, common)는 도메인 폴더 밖에 둔다.

## 2. 컨트롤러 작성 규칙

### 기본 형태

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** 회원 가입. */
    @PostMapping
    public ApiResponse<UserJoinResponse> join(@Valid @RequestBody UserJoinRequest request) {
        return ApiResponse.success(userService.join(request));
    }
}
```

### 규칙

1. **컨트롤러는 얇게 유지한다.** 요청을 서비스에 전달하고 결과를 `ApiResponse`로 감싸는 것 외의 로직을 넣지 않는다. 요청 DTO 형식 검증은 `@Valid` + Bean Validation이 담당하고, 상태 전환·비즈니스 예외 판단은 서비스가 담당한다.
2. **응답은 `ApiResponse<T>` 봉투로 감싼다.** `ApiResponse.success(data)`를 사용하면 traceId/timestamp가 자동으로 채워진다.
   - 모든 컨트롤러에 적용된다. 인증 엔드포인트(`AuthController.login`)도 예외 없이 봉투를 사용한다.
3. **본문 없는 성공은 `ApiResponse<Void>`**로 `ApiResponse.success(null)`을 반환한다 (예: 회원 탈퇴).
4. **인증 주체는 `Authentication` 파라미터로 받는다.** 서비스에는 `authentication.getName()`(로그인 ID)만 넘긴다. `UserPrincipal` 등 내부 구현체를 컨트롤러에서 다루지 않는다.
5. **접근자 없이 생성자 주입**을 쓴다. `@RequiredArgsConstructor` + `private final` 필드.
6. **클래스/메서드에는 한국어 Javadoc**을 작성한다. 메서드는 `/** 회원 가입. */`처럼 한 줄 요약, 복잡한 동작은 여러 줄로.

### 매핑 규칙

## 3. 서비스(비즈니스 로직) 작성 규칙

### 기본 형태

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserJoinResponse join(UserJoinRequest request) {
        if (userRepository.existsByLoginId(request.id())) {
            throw new IllegalStateException("이미 사용 중인 로그인 ID입니다. id=" + request.id());
        }
        UserEntity user = userRepository.save(UserEntity.builder() ... .build());
        return UserJoinResponse.from(user);
    }
}
```

### 규칙

1. **트랜잭션 어노테이션**
   - 상태 변경(쓰기) 메서드: `@Transactional`
   - 조회 전용 메서드: `@Transactional(readOnly = true)`
2. **예외는 표준 예외로 던지고, GlobalExceptionHandler가 매핑한다.**
   - `IllegalStateException` → 409 Conflict (`DUPLICATE_STATE`)
   - `IllegalArgumentException` → 400 Bad Request (`INVALID_ARGUMENT`)
   - 그 외 예외 → 500 (내부 메시지 노출 금지, 핸들러가 범용 메시지로 변환)
   - 커스텀 예외 클래스를 새로 만들기 전에 표준 예외로 충분한지 먼저 판단한다.
3. **예외 메시지에는 식별 정보를 포함한다.** `"사용자를 찾을 수 없습니다. id=" + loginId`처럼 원인 추적이 가능하게. 단, 응답으로 노출돼도 되는 정보만 넣는다(비밀번호 등 금지).
4. **서비스가 DTO/엔티티 경계를 책임진다.**
   - 요청 DTO → 엔티티 변환과 저장은 서비스에서 수행 (빌더 사용)
   - 엔티티 → 응답 DTO 변환은 DTO의 정적 팩토리(`UserJoinResponse.from(user)`) 사용
5. **비밀번호 등 민감 데이터는 반드시 인코딩 후 저장** (`passwordEncoder.encode`). 평문 비교는 `passwordEncoder.matches`로만.
6. **도메인 상태 변경은 엔티티 메서드로 캡슐화한다.** `user.withdraw()`처럼 엔티티가 자신의 상태 전환과 부수 필드 기록(withdrawnAt)을 담당하며, 서비스에서 상태 필드를 직접 세팅하지 않는다.
7. **응답 노출 필드를 제한한다.** 응답 DTO에는 PK와 화면에 필요한 필드만 담고, 비밀번호·상태 코드 등 내부 필드는 노출하지 않는다.

## 4. DTO 작성 규칙

- **불변 record**로 작성한다. 클래스당 하나의 목적(요청 or 응답)만.
- 파일명: `<기능>Request` / `<기능>Response` (예: `LoginRequest`, `WithdrawRequest`).
- 각 필드에 `@param` Javadoc을 남긴다.
- **요청 DTO에는 Bean Validation을 적용하는 것을 권장한다.** 신뢰 경계(외부 입력)이므로 컨트롤러의 `@Valid @RequestBody`에서 검증되고, 실패 시 `GlobalExceptionHandler`가 400 + `VALIDATION_ERROR` + 필드별 오류로 응답한다.
  - 필수 문자열: `@NotBlank`, 크기 제한: `@Size(max = 엔티티 컬럼 길이)` — 컬럼 정의와 일치시킨다
  - 부분 수정(PATCH) DTO처럼 "null은 변경 없음"인 경우 `@NotBlank`를 쓰지 않고 `@Size`/`@Min`처럼 null을 통과시키는 규칙만 적용한다 (`CodeUpdateRequest` 참고)
  - 교차 필드 규칙(예: 비밀번호 정책)은 클래스 레벨 커스텀 제약으로 처리한다 (`user/validation/StrongPassword` 참고)
  - 로그인처럼 포맷 정보 유출을 피해야 하는 요청은 `@NotBlank`만 적용한다

```java
/**
 * 회원 가입 요청 DTO.
 *
 * @param id 로그인 ID
 * @param pw 비밀번호 (평문, 서비스에서 BCrypt로 인코딩해 저장)
 */
public record UserJoinRequest(
        @NotBlank(message = "아이디는 필수입니다.") @Size(max = 50, message = "아이디는 50자 이하여야 합니다.") String id,
        @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.") String pw,
        @NotBlank(message = "이름은 필수입니다.") @Size(max = 50, message = "이름은 50자 이하여야 합니다.") String userName) {
}
```

- 응답 DTO는 엔티티에서 변환하는 정적 팩토리를 제공한다:

```java
public static UserJoinResponse from(UserEntity user) {
    return new UserJoinResponse(user.getId(), user.getUserName());
}

## 5. 엔티티 & 리포지토리 작성 규칙

### 엔티티

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // SQLite AUTOINCREMENT
    private Long seq;
    ...
}
```

- Lombok 조합: `@Getter` + `@NoArgsConstructor(PROTECTED)` + `@AllArgsConstructor(PRIVATE)` + `@Builder`.
- 상태(enum)는 코드값 컨버터(`@Convert`)로 저장하고, 컬럼에 DB 기본값이 필요하면 `columnDefinition`을 명시한다.
- 비즈니스 상태 전환 메서드를 엔티티 안에 둔다 (`withdraw()`).

### 공통 코드 (common/code)

코드값을 DB에서 동적으로 관리하되, 비즈니스 로직에 필요한 코드는 enum의 타입 세이프함을 유지하는 하이브리드 구조다.

#### 패키지 구조

```
com.template.core.common.code
├── CodeEnum.java          # 코드성 enum의 공통 계약(인터페이스)
├── CodeSyncRunner.java    # 기동 시 enum → DB 싱크(ApplicationRunner)
├── entity/                # CodeEntity(복합PK: group_code+code), CodeGroupEntity, CodeId
├── repository/            # CodeRepository, CodeGroupRepository
├── service/               # CodeService (캐시 + 트리 조립 + 관리 동작)
├── controller/            # CodeAdminController (/admin/codes, ROLE_ADMIN)
└── dto/                   # 생성/수정 요청, 트리 응답 record
```

#### 테이블 구조

```
code_groups (그룹)                    codes (값, 복합 PK)
├── group_code   PK                   ├── group_code + code  복합 PK (CodeId)
├── group_name                        ├── parent_code        ← 같은 그룹 내 상위 코드(계층의 핵심)
├── description                       ├── name               (코드명, 예: ACTIVE)
├── use_yn                            ├── description        (설명, 예: 활성화회원)
└── seed_yn                           ├── sort_order
                                      ├── use_yn             (false = 비활성, soft delete)
                                      └── seed_yn            (true = enum에서 유래)
```

#### 계층(2/3레벨) 표현 방식

`codes.parent_code` 자기참조로 N계층을 표현한다. `parent_code`가 null이면 그룹 1레벨이고, 같은 **그룹 내** 상위 코드만 가리킬 수 있다(타 그룹 참조는 생성 시 거부됨).

```
group: MENU
├── 100  대분류          (parent = null, 1레벨)
│   ├── 110  중분류      (parent = 100, 2레벨)
│   │   └── 111  소분류  (parent = 110, 3레벨)
│   └── 120  숨김분류    (use_yn = false)
└── 200  대분류2         (parent = null)
```

#### enum ↔ DB 싱크 규칙 (단방향: enum → DB)

1. 비즈니스 동작에 필요한 코드는 enum(`CodeEnum` 구현)으로 정의한다. 기존 `UserStatus`, `UserRole`처럼 코드 컨버터(`@Convert`)로 저장하는 방식은 그대로 유지된다.
2. 애플리케이션 기동 시 `CodeSyncRunner`가 enum 값을 `code_groups`/`codes` 테이블로 upsert한다. 이때 생성/갱신되는 행은 `seed_yn = true`다.
3. **seed 코드는 관리 API로 수정·비활성화할 수 없다.** enum이 원본이므로 재배포 시 싱크가 원본 값을 덮어쓴다(이름/설명/정렬 불일치 감지 시 자동 갱신).
4. DB에 같은 코드값의 비시드 코드(관리 API로 만든 동적 코드)가 이미 있으면 덮어쓰지 않고 경고 로그만 남긴다.
5. **동적 코드(enum에 없는 코드)는 비즈니스 분기(`==` 비교)에 사용할 수 없다.** 이름·설명 조회, 트리 표시 등 표시 목적으로만 사용한다.
6. enum 안에서 2레벨 계층을 시딩하려면 `getParentCode()`를 오버라이드하고, 상위 코드 상수를 목록에서 먼저 선언한다(선언 순서가 어긋나도 러너가 반복 시도로 해소).

#### 새 코드 enum 추가 절차

```java
// 1. enum 정의 (코드 컨버터가 있다면 기존 패턴 그대로 유지)
@Getter
public enum MenuType implements CodeEnum {
    BASIC(10, "기본"),
    PREMIUM(20, "프리미엄");

    private final int code;
    private final String description;
    ...
}

// 2. CodeSyncRunner.SEED_CODES에 상수 등록 (한 줄)
private static final List<CodeEnum> SEED_CODES = List.of(
        UserStatus.ACTIVE, UserStatus.WITHDRAWN,
        UserRole.ROLE_USER, UserRole.ROLE_ADMIN,
        MenuType.BASIC, MenuType.PREMIUM);
```

- 그룹 식별자는 enum 클래스명에서 자동 파생된다(`MenuType` → `MENU_TYPE`).
- 그룹 표시 이름을 한글로 하려면 `getGroupName()`을 오버라이드한다(기본값은 그룹 식별자).

#### 조회 API (비즈니스 코드에서 사용)

`CodeService`를 주입받아 사용한다. 조회는 메모리 캐시로 처리되므로 코드 테이블을 직접 repository로 조회하지 않는다.

```java
private final CodeService codeService;

// 단건 활성 코드 조회
Optional<CodeEntity> code = codeService.getCode("USER_STATUS", "10");

// 직속 자식 조회 (parentCode가 null이면 그룹 1레벨)
List<CodeEntity> children = codeService.getChildren("MENU", "100");

// 활성 트리 조회 (2/3레벨이 children에 재귀 중첩)
List<CodeTreeResponse> tree = codeService.getTree("MENU");
```

#### 관리 API (/admin/codes — ROLE_ADMIN)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/admin/codes` | 전체 그룹 + 각 그룹의 코드 트리(비활성 포함) |
| POST | `/admin/codes/groups` | 새 그룹 생성 (`{groupCode, groupName, description}`) |
| POST | `/admin/codes/{groupCode}/codes` | 코드 생성 (`{code, parentCode?, name, description?, sortOrder?}`) — `parentCode` 지정 시 2레벨 이상 |
| PATCH | `/admin/codes/{groupCode}/{code}` | 이름/설명/정렬/사용여부 수정 (null 필드는 변경 안 함, seed 코드는 거부) |
| DELETE | `/admin/codes/{groupCode}/{code}` | 비활성화(soft delete, 물리 삭제 없음) — seed 코드·활성 하위 코드 보유 시 거부 |

- 검증 실패는 `IllegalArgumentException`(400)/`IllegalStateException`(409)로 처리되며 `GlobalExceptionHandler`가 공통 응답으로 변환한다. 요청 본문 형식 검증은 컨트롤러의 `@Valid` + Bean Validation이 담당하며, 실패 시 400 `VALIDATION_ERROR`와 필드별 오류(`fieldErrors`)로 응답한다.
- 코드 변경 시 `CodeService`가 캐시를 무효화하므로 다음 조회는 변경된 값이 반영된다.

#### 주의사항

- `CodeService` 캐시는 코드 테이블 전체를 메모리에 적재한다(`ponytail:` 수만 건 이상으로 커지면 그룹 단위 지연 로딩으로 교체).
- **비즈니스 코드 분기는 반드시 enum으로 한다**: `user.getStatus() == UserStatus.WITHDRAWN`. DB 코드값만으로 문자열 비교 분기를 만들지 않는다.
- 새 코드 enum 추가 시 반드시 `CodeSyncRunner.SEED_CODES`에 등록한다. 누락하면 DB로 싱크되지 않는다.
- 참조 테스트: `src/test/java/com/template/core/common/code/CodeSyncRunnerTest.java`, `CodeServiceTest.java`


### 리포지토리

- `JpaRepository` 상속 + `@Repository`.
- PK(seq) 이외의 컬럼으로 조회할 때는 파생 메서드 대신 **JPQL `@Query`로 명시**해 `findById`와 충돌하지 않게 한다 (`findByLoginId` 참고).
- 벌크 삭제/수정은 `@Modifying` + JPQL.

## 6. 예외 처리 흐름

```
Service → throw IllegalArgumentException/IllegalStateException
        → GlobalExceptionHandler (@RestControllerAdvice)
        → ErrorResponse(code, message) 를 ApiResponse.error 로 감싸 응답
```

- 4xx 응답에는 원인 메시지를 그대로 노출, 500은 범용 메시지로 치환(내부 정보 보호).
- traceId는 `TraceIdFilter`가 MDC에 넣은 값을 `ApiResponse`가 자동으로 실어준다.

## 7. 테스트 작성 규칙

서비스 단위 테스트는 Mockito로 의존성을 격리한다:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;
}
```

- 검증은 AssertJ (`assertThat`, `assertThatThrownBy`), JUnit5 + `@DisplayName` 한국어.
- 메서드명은 영문 `동작_조건_결과` 형식: `join_WithDuplicateLoginId_ThrowsException`.
- given/when/then 주석 구조를 유지한다.
- 엔티티 상태 전환·컨버터 등 도메인 로직은 `UserEntityTest`처럼 별도 단위 테스트로.

## 8. 새 기능 추가 체크리스트

1. 도메인 패키지 폴더 생성 (`controller/dto/entity/repository/service`)
2. 엔티티 + 리포지토리 (JPQL로 컬럼 조회 메서드 명시)
3. 요청/응답 DTO record (응답 DTO에 `from` 팩토리, 요청 DTO에 검증 어노테이션 + 컨트롤러 `@Valid` — §4 참고)
4. 서비스: `@Transactional` (+readOnly), 표준 예외 throw
5. 컨트롤러: `ApiResponse.success(...)` 봉투, 얇은 위임
6. 단위 테스트: 정상 케이스 + 각 예외 케이스
7. 필요 시 `GlobalExceptionHandler`에 새 예외 매핑 추가
8. `./gradlew test` 전체 통과 확인

## 9. 공통 규칙

- 주석·Javadoc·로그 메시지는 **한국어**.
- Lombok은 관례대로 사용하되, 컨트롤러/서비스는 생성자 주입(`@RequiredArgsConstructor`)만.
- 설정 주입은 `@ConfigurationProperties` 레코드(`JwtProperties`, `WithdrawalProperties` 참고)를 사용하고 `@Value` 개별 주입은 하지 않는다.
- API 응답/에러 형식을 바꿀 때는 `ApiResponse`, `ErrorResponse`, `GlobalExceptionHandler` 세 파일을 함께 점검한다.

```


- 경로는 `/api/<복수형 도메인>` (`/api/users`, `/api/auth`).
- 상태 변경은 POST, 본인 리소스 삭제는 `@DeleteMapping("/me")`처럼 REST 스타일을 유지한다.
