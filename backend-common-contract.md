# MCM Private Circle 백엔드 공통 구현 6가지 확정사항

> 기준 문서: `PROJECT.md`, `MCM_ERD_v6.dbml`  
> 목적: 백엔드 2인이 기능 개발을 시작하기 전에 공통 구현 방식을 하나로 통일한다.  
> 상태: **팀 공통 구현 기준으로 확정**

---

# 1. 공통 응답 DTO — 확정

## 결정

공통 응답은 다음 두 객체를 사용한다.

```text
ApiResponse<T>
ErrorDetail
```

`ApiResponse<T>` 하나에서 성공/실패 응답을 모두 표현하며, `null` 필드는 JSON 응답에서 제외한다.

## 권장 구조

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final ErrorDetail error;

    private ApiResponse(
            boolean success,
            T data,
            String message,
            ErrorDetail error
    ) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                data,
                "요청이 성공했습니다.",
                null
        );
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
                true,
                data,
                message,
                null
        );
    }

    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        return new ApiResponse<>(
                false,
                null,
                null,
                error
        );
    }
}
```

```java
public record ErrorDetail(
        String code,
        String message
) {
}
```

## 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공했습니다."
}
```

## 실패 응답

```json
{
  "success": false,
  "error": {
    "code": "CUSTOMER_NOT_FOUND",
    "message": "고객을 찾을 수 없습니다."
  }
}
```

## 구현 규칙

- Controller에서 Entity를 직접 반환하지 않는다.
- 각 기능의 Response DTO를 `ApiResponse<T>`의 `data`에 넣는다.
- 성공 응답에는 `error`를 노출하지 않는다.
- 실패 응답에는 `data`, 최상위 `message`를 노출하지 않는다.
- `GlobalExceptionHandler`도 동일한 실패 응답 구조를 사용한다.

## 위치

```text
global/response
├── ApiResponse.java
└── ErrorDetail.java
```

---

# 2. 공통 ErrorCode Enum — 확정

## 결정

MVP에서는 ErrorCode를 도메인별 여러 Enum으로 쪼개지 않고 **단일 `ErrorCode` Enum**으로 관리한다.

각 ErrorCode는 다음 세 값을 가진다.

```text
HttpStatus
code
message
```

## 권장 구조

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    CUSTOMER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CUSTOMER_NOT_FOUND",
            "고객을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

## 예외 구조

```text
global/exception
├── ErrorCode.java
├── BusinessException.java
└── GlobalExceptionHandler.java
```

비즈니스 예외는 가능한 한 `BusinessException(ErrorCode)` 형태로 통일한다.

## MVP 공통 ErrorCode

| ErrorCode | HTTP Status | 의미 |
|---|---:|---|
| `INVALID_REQUEST` | 400 | 요청값 검증 실패 |
| `INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `TOKEN_EXPIRED` | 401 | JWT 만료 |
| `INVALID_TOKEN` | 401 | JWT 오류 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `FORBIDDEN_CA` | 403 | 권한 없는 CA |
| `ACCOUNT_NOT_FOUND` | 404 | 계정 없음 |
| `CUSTOMER_NOT_FOUND` | 404 | 고객 없음 |
| `CA_NOT_FOUND` | 404 | CA 프로필 없음 |
| `STORE_NOT_FOUND` | 404 | 매장 없음 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |
| `VISIT_NOT_FOUND` | 404 | 방문 없음 |
| `VISIT_RECORD_NOT_FOUND` | 404 | 방문 기록 없음 |
| `DUPLICATE_LOGIN_ID` | 409 | 로그인 ID 중복 |
| `DUPLICATE_PHONE_NUMBER` | 409 | 전화번호 중복 |
| `DUPLICATE_QR_TOKEN` | 409 | QR Token 중복 |
| `DUPLICATE_INTEREST_PRODUCT` | 409 | 관심 제품 중복 |
| `VISIT_RECORD_ALREADY_EXISTS` | 409 | 방문 기록 중복 |
| `STAMP_ALREADY_ISSUED` | 409 | 스탬프 중복 발급 |
| `PRODUCT_IN_USE` | 409 | 참조 중인 상품 삭제 시도 |
| `INVALID_INTEREST_SOURCE` | 400 | 관심 제품 출처 조합 오류 |
| `VISIT_CUSTOMER_MISMATCH` | 400 | 방문 고객 불일치 |
| `AI_API_TIMEOUT` | 502 | 외부 AI API 시간 초과 |
| `AI_RESPONSE_PARSE_FAILED` | 502 | AI 응답 구조/파싱 실패 |

## 구현 규칙

- 동일 의미의 ErrorCode를 새로 중복 생성하지 않는다.
- ErrorCode 추가 전 기존 Enum에 같은 의미의 코드가 있는지 확인한다.
- HTTP Status, API Error Code, 사용자 메시지를 Controller마다 직접 작성하지 않는다.
- `GlobalExceptionHandler`가 `ErrorCode`를 기준으로 응답을 생성한다.

---

# 3. JWT 인증 사용자 객체 — 확정

## 결정

Spring Security에서 인증된 사용자를 표현하는 객체는 **단일 `AuthenticatedUser`**로 통일한다.

고객과 CA 모두 같은 Principal 타입을 사용하되, 생성 Factory를 분리하여 잘못된 ID 조합을 최대한 방지한다.

## 애플리케이션 Role

```java
public enum UserRole {
    CUSTOMER,
    CA
}
```

중요:

- `UserRole`은 **DB에 저장하는 Role 컬럼이 아니다.**
- 현재 ERD에 Role 컬럼을 추가하지 않는다.
- 고객 계정과 직원 계정이 분리되어 있으므로 로그인한 계정 종류를 기반으로 애플리케이션에서 Role을 부여한다.

## JWT Role Claim

JWT의 `role` Claim에는 다음 값을 저장한다.

```text
CUSTOMER
CA
```

Spring Security Authority 변환 시 다음처럼 사용한다.

```text
CUSTOMER → ROLE_CUSTOMER
CA       → ROLE_CA
```

예:

```java
new SimpleGrantedAuthority(
        "ROLE_" + authenticatedUser.getRole().name()
);
```

## AuthenticatedUser 구조

```java
@Getter
public class AuthenticatedUser {

    private final Long accountId;
    private final Long customerId;
    private final Long caId;
    private final Long storeId;
    private final UserRole role;

    private AuthenticatedUser(
            Long accountId,
            Long customerId,
            Long caId,
            Long storeId,
            UserRole role
    ) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.caId = caId;
        this.storeId = storeId;
        this.role = role;
    }

    public static AuthenticatedUser customer(
            Long accountId,
            Long customerId
    ) {
        return new AuthenticatedUser(
                accountId,
                customerId,
                null,
                null,
                UserRole.CUSTOMER
        );
    }

    public static AuthenticatedUser ca(
            Long accountId,
            Long caId,
            Long storeId
    ) {
        return new AuthenticatedUser(
                accountId,
                null,
                caId,
                storeId,
                UserRole.CA
        );
    }
}
```

## 고객 JWT Claim

```text
accountId
customerId
role = CUSTOMER
```

## CA JWT Claim

```text
accountId
caId
storeId
role = CA
```

## 권한 검증 규칙

- CUSTOMER 기능에서는 인증 객체의 `customerId`를 사용한다.
- CA 기능에서는 인증 객체의 `caId`, `storeId`를 사용한다.
- Request Body 또는 Path에서 전달된 `caId`만 신뢰하지 않는다.
- Spring Security Endpoint 접근 제어와 Service 계층의 리소스 권한 검증을 모두 적용한다.

## 위치

```text
global/security
├── AuthenticatedUser.java
├── UserRole.java
├── JwtTokenProvider.java
├── JwtAuthenticationFilter.java
└── SecurityConfig.java
```

---

# 4. Entity BaseTime 정책 — 확정

## 결정

공통 `created_at` 처리는 **`BaseTimeEntity` + Spring Data JPA Auditing**으로 통일한다.

BaseTimeEntity에는 `createdAt`만 둔다.

`updatedAt`은 현재 ERD 공통 컬럼이 아니므로 임의로 추가하지 않는다.

## BaseTimeEntity

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

## JPA Auditing

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

## BaseTimeEntity 적용 대상

ERD에 `created_at` 컬럼이 있는 Entity만 상속한다.

```text
CustomerAccount
EmployeeAccount
Store
Customer
ClientAdvisor
Product
VisitRecord
```

## BaseTimeEntity에 포함하지 않는 시간값

다음 값들은 생성 시각이 아니라 각각 별도의 비즈니스 의미가 있으므로 공통 BaseTimeEntity로 처리하지 않는다.

```text
customers.joined_at
customer_interest_products.saved_at
visits.visited_at
purchase_history.purchased_at
visit_stamps.issued_at
ai_journey_briefs.generated_at
```

각 Entity의 명시적 필드로 관리한다.

## 시간 기준

- Java 타입: `LocalDateTime`
- DB 타입: `datetime`
- API: ISO-8601
- 서비스 시간 기준: KST

---

# 5. 패키지명 — 확정

## Root Package

다음으로 통일한다.

```text
com.mcm.privatecircle
```

## 기본 패키지 구조

```text
src/main/java/com/mcm/privatecircle
├── PrivateCircleApplication.java
│
├── global
│   ├── config
│   ├── security
│   ├── exception
│   ├── response
│   ├── common
│   │   └── entity
│   └── util
│
├── auth
│   ├── controller
│   ├── service
│   └── dto
│
├── account
│   ├── entity
│   └── repository
│
├── customer
├── employee
├── store
├── product
├── visit
├── interest
├── purchase
├── stamp
│
└── ai
    ├── controller
    ├── service
    ├── dto
    ├── entity
    ├── repository
    └── client
```

## 공통 클래스 위치

```text
global/config/JpaConfig.java

global/common/entity/BaseTimeEntity.java

global/response/ApiResponse.java
global/response/ErrorDetail.java

global/exception/ErrorCode.java
global/exception/BusinessException.java
global/exception/GlobalExceptionHandler.java

global/security/AuthenticatedUser.java
global/security/UserRole.java
global/security/JwtTokenProvider.java
global/security/JwtAuthenticationFilter.java
global/security/SecurityConfig.java
```

## 규칙

- 도메인별 코드는 해당 도메인 패키지에 둔다.
- 모든 Entity를 한 개의 전역 Entity 패키지에 몰아넣지 않는다.
- 공통 코드만 `global`에 둔다.
- 비즈니스 기능을 `global`에 넣지 않는다.

---

# 6. API Prefix — 확정

모든 외부 REST API의 공통 Prefix는 다음으로 통일한다.

```text
/api/v1
```

예:

```http
POST /api/v1/auth/customers/signup
POST /api/v1/auth/customers/login
POST /api/v1/auth/employees/login

GET /api/v1/customers/me
GET /api/v1/stores
GET /api/v1/products
POST /api/v1/visits
POST /api/v1/purchases
```

## 규칙

- 각 Controller가 `/api/v1`을 서로 다른 형태로 임의 변경하지 않는다.
- `/api`, `/v1`, `/api/v1`이 혼재하지 않도록 한다.
- 현재 MVP API는 `/api/v1`을 사용한다.
- 새로운 API Version이 실제로 필요해질 때만 `/api/v2`를 검토한다.

---

# 7. 최종 확정 요약

| 항목 | 최종 결정 |
|---|---|
| 공통 응답 DTO | `ApiResponse<T>` + `ErrorDetail`, NULL 필드 JSON 제외 |
| 공통 ErrorCode | 단일 `ErrorCode` Enum + `HttpStatus/code/message` |
| 공통 예외 처리 | `BusinessException` + `GlobalExceptionHandler` |
| JWT 인증 객체 | 단일 `AuthenticatedUser` |
| 애플리케이션 Role | `UserRole.CUSTOMER`, `UserRole.CA` |
| JWT Role Claim | `CUSTOMER`, `CA` |
| Spring Security Authority | `ROLE_CUSTOMER`, `ROLE_CA` |
| DB Role 컬럼 | 추가하지 않음 |
| BaseTime | `BaseTimeEntity` + JPA Auditing |
| BaseTime 공통 필드 | `createdAt`만 |
| Root Package | `com.mcm.privatecircle` |
| API Prefix | `/api/v1` |

---

# 8. 검증 결과 및 추가 정리 필요사항

## 8.1 6개 결정 자체의 충돌 여부

**6개 결정은 서로 충돌하지 않으며 함께 적용 가능하다.**

공통 응답 → ErrorCode → GlobalExceptionHandler가 자연스럽게 연결되고, JWT Principal과 패키지 구조도 현재 고객/CA 분리 구조와 충돌하지 않는다.

Role은 DB 컬럼이 아니라 Spring Security/JWT 애플리케이션 레벨 구분값으로 확정했으므로 현재 ERD에 Role 컬럼을 추가할 필요가 없다.

---

## 8.2 ErrorCode 정합성 검증 — 해결 완료

기존 검토에서 `PROJECT.md` 본문에는 사용되고 있었지만 공통 ErrorCode 목록에는 누락되어 있던 다음 코드의 정합성 문제가 확인되었다.

```text
DUPLICATE_QR_TOKEN
PRODUCT_IN_USE
FORBIDDEN_CA
```

현재 `PROJECT.md`의 공통 ErrorCode 목록에 위 세 코드가 모두 반영되었으며, 본 문서에서 확정한 `ErrorCode` 목록과 동일하게 정리되었다.

따라서 ErrorCode 관련 문서 누락 문제는 **해결 완료**로 판단한다.

현재 공통 구현에서는 다음 원칙을 유지한다.

* ErrorCode는 단일 `ErrorCode` Enum으로 관리한다.
* 각 ErrorCode는 `HttpStatus`, `code`, `message`를 가진다.
* 기존 ErrorCode와 동일한 의미의 코드를 중복 생성하지 않는다.
* 새로운 ErrorCode가 필요한 경우 기존 목록과 중복 여부를 먼저 확인한다.
* `GlobalExceptionHandler`는 `ErrorCode`를 기준으로 공통 실패 응답을 생성한다.

---

## 8.3 `created_at` 및 `joined_at` NULL 제약 정합성 검증 — 해결 완료

기존 검토에서 `PROJECT.md`의 시간 컬럼 정책과 `MCM_ERD_v6.dbml`의 NULL 제약이 일부 일치하지 않는 문제가 확인되었다.

현재 ERD에는 다음 컬럼의 `NOT NULL` 제약이 반영되어 있다.

```text
customer_accounts.created_at
employee_accounts.created_at
stores.created_at
customers.joined_at
customers.created_at
client_advisors.created_at
products.created_at
visit_records.created_at
```

이에 따라 `PROJECT.md`에서 정의한 해당 컬럼의 필수값 정책과 ERD의 NULL 제약이 일치하도록 정리되었다.

따라서 기존의 `created_at` 및 `customers.joined_at` NULL 제약 불일치 문제는 **해결 완료**로 판단한다.

공통 시간 처리 정책은 기존 확정사항을 그대로 유지한다.

```text
BaseTimeEntity
└── createdAt
```

* ERD에 `created_at` 컬럼이 존재하는 Entity는 `BaseTimeEntity`를 통해 `createdAt`을 관리한다.
* `BaseTimeEntity`는 Spring Data JPA Auditing을 사용한다.
* `updatedAt`은 현재 ERD의 공통 컬럼이 아니므로 임의로 추가하지 않는다.
* `joinedAt`, `visitedAt`, `savedAt`, `purchasedAt`, `issuedAt`, `generatedAt`은 각각 별도의 비즈니스 의미를 가지므로 각 Entity에서 직접 관리한다.

---

## 8.4 추가로 확정해야 하는 핵심 항목

**이번 6개 공통 구현 사항을 적용하기 위해 추가로 반드시 결정해야 하는 핵심 항목은 없다.**

다만 구현 단계에서 다음은 코드 세부사항으로 정하면 된다.

- Lombok 사용 여부
- `ApiResponse`를 일반 class로 둘지 Java `record`로 둘지
- JWT 라이브러리의 실제 구현체 및 세부 메서드
- JPA Auditing 테스트 방식

위 항목들은 현재 6개 공통 계약을 바꾸는 정책 결정이 아니라 구현 세부사항이다.

---

### AI 공급자 정책과의 경계

이 문서는 공통 응답, ErrorCode, JWT Principal, BaseTime, Package, API Prefix만 확정한다.

Gemini 모델, AI 입력 데이터, Prompt, Structured Output, API Key 등 AI 공급자별 구현 정책은 `PROJECT.md`, `backend-rules.md`, `AGENTS.md`를 따른다.

따라서 AI 공급자가 변경되더라도 본 문서의 6개 공통 계약을 임의로 변경하지 않는다.

---

# 9. 팀 공통 적용 규칙

두 백엔드 담당자는 기능 구현 전에 이 문서의 6개 항목을 공통 기준으로 사용한다.

기존 `backend-rules.md`와 `AGENTS.md`에 동일한 내용이 존재해도 삭제할 필요는 없다.

다만 서로 다른 구현 방식이 적혀 있을 경우 이 문서의 확정사항에 맞춰 통일한다.

```text
공통 응답
공통 ErrorCode
JWT Principal
BaseTime
Package
API Prefix
```

위 6개는 각 담당자가 개별 방식으로 다시 정의하지 않는다.
