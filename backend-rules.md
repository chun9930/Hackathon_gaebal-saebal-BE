---
trigger: always_on
---

# MCM Private Circle Backend Rules

## 1. 적용 기준

이 문서는 MCM Private Circle MVP 백엔드 개발 시 항상 적용한다.

작업 전 다음 파일을 기준으로 확인한다.

1. `PROJECT.md`
2. `MCM_ERD_v6.dbml`
3. `backend-common-contract.md`
4. 현재 구현 코드
5. 사용자가 이후 명시적으로 확정한 최신 결정사항

`PROJECT.md`와 `MCM_ERD_v6.dbml`에 없는 기능, 테이블, 권한, 상태값은 임의로 추가하지 않는다.

문서와 스키마 또는 코드 사이에 충돌이 발견되면 임의로 추측하여 구현하지 말고 충돌 내용을 먼저 보고한다.

---

## 2. 기술 스택

- Java
- Spring Boot
- Gradle
- MySQL
- Spring Data JPA
- Spring Security + JWT
- REST API
- Google Gemini API
- Swagger / OpenAPI
- JUnit 5
- Spring Boot Test
- Mockito

Java 또는 Spring Boot의 실제 세부 버전은 프로젝트 설정 파일에 정의된 값을 따른다.

---

## 3. 아키텍처 규칙

도메인 기반 패키지 구조를 사용한다.

```text
src/main/java/com/mcm/privatecircle
├── global
│   ├── config
│   ├── security
│   ├── exception
│   ├── response
│   └── util
├── auth
├── account
├── customer
├── employee
├── store
├── product
├── visit
├── interest
├── purchase
├── stamp
└── ai
    └── client
```

기본 호출 흐름은 다음을 따른다.

```text
Controller → Service → Repository
```

### Controller

- HTTP 요청/응답만 담당한다.
- Request DTO 검증을 수행한다.
- 인증 사용자 정보를 Service에 전달한다.
- Entity를 직접 반환하지 않는다.
- 비즈니스 로직을 작성하지 않는다.

### Service

- 비즈니스 규칙을 처리한다.
- 인증/인가를 최종 검증한다.
- FK 및 도메인 정합성을 검증한다.
- 트랜잭션 경계를 관리한다.
- 공통 예외를 발생시킨다.

### Repository

- Spring Data JPA 기반 DB 접근만 담당한다.
- 비즈니스 정책을 Repository에 넣지 않는다.

### DTO

- 모든 Request / Response는 DTO를 사용한다.
- Entity를 API 계약으로 사용하지 않는다.

---

## 4. 계정 및 권한 규칙

MVP 계정은 고객과 직원/CA로 분리한다.

### 고객

```text
customer_accounts
        │ 1:1
        ▼
customers
```

권한:

```text
ROLE_CUSTOMER
```

고객은 일반 회원가입이 가능하다.

### 직원 / CA

```text
employee_accounts
        │ 1:1
        ▼
client_advisors
```

권한:

```text
ROLE_CA
```

직원은 공개 회원가입을 제공하지 않는다.

직원 계정은 DB seed 또는 프로젝트에서 합의한 내부 방식으로 준비한다.

### 관리자

관리자 권한 및 관리자 백오피스 고도화는 MVP 범위에 포함하지 않는다.

관리자 Role, 관리자 회원가입, 승인 절차를 임의로 추가하지 않는다.

---

## 5. 인증 규칙

### 고객 회원가입

```http
POST /api/v1/auth/customers/signup
```

필수 입력:

- `loginId`: 4~100자, 중복 불가
- `password`: 8~64자
- `name`: 1~100자
- `phoneNumber`: 10~30자, 중복 불가

회원가입 시 하나의 비즈니스 작업으로 생성한다.

- `customer_accounts`
- `customers`
- `customers.qr_token`
- `joined_at`
- `created_at`

`qr_token`은 서버가 UUID 또는 보안 난수 기반으로 생성한다.

클라이언트가 QR Token을 직접 지정하도록 하지 않는다.

### 고객 로그인

```http
POST /api/v1/auth/customers/login
```

JWT Claim:

```text
accountId
customerId
role
```

### 직원 로그인

```http
POST /api/v1/auth/employees/login
```

JWT Claim:

```text
accountId
caId
storeId
role
```

### JWT

- Bearer Access Token 사용
- Access Token 만료: 2시간
- Refresh Token: MVP 제외
- 비밀번호: BCrypt
- 인증 Header:

```http
Authorization: Bearer {token}
```

### 인증 오류

- 만료: `TOKEN_EXPIRED`
- 위조/형식 오류: `INVALID_TOKEN`
- 로그인 실패: `INVALID_CREDENTIALS`

로그인 실패 시 ID 존재 여부나 비밀번호 불일치 여부를 구체적으로 노출하지 않는다.

---

## 6. 보안 및 설정 규칙

다음 값을 GitHub에 커밋하지 않는다.

- JWT Secret
- Google Gemini API Key
- 실제 운영 DB 비밀번호
- 기타 Secret

Secret은 다음 방식 중 프로젝트에서 채택한 방식을 사용한다.

- 환경변수
- `.env`
- Git에서 제외한 `application-secret.yml`

평문 비밀번호와 `password_hash`를 로그에 남기지 않는다.

---

## 7. API 공통 규칙

Base URL:

```text
/api/v1
```

REST 의미를 지킨다.

- 생성: `POST`
- 조회: `GET`
- 부분 수정: `PATCH`
- 삭제: `DELETE`

### 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공했습니다."
}
```

### 실패 응답

```json
{
  "success": false,
  "error": {
    "code": "CUSTOMER_NOT_FOUND",
    "message": "고객을 찾을 수 없습니다."
  }
}
```

### HTTP Status

- `200`: 조회/수정/삭제 성공
- `201`: 생성 성공
- `400`: 요청값 또는 도메인 검증 실패
- `401`: 인증 실패
- `403`: 권한 없음
- `404`: 리소스 없음
- `409`: 중복/상태 충돌
- `500`: 서버 내부 오류
- `502`: 외부 AI API 연동 실패

### 날짜/시간

- Java: `LocalDateTime`
- DB: `datetime`
- API: ISO-8601
- 서버 시간 기준: KST

---

## 8. 고객 도메인 규칙

`customers`는 고객 프로필을 저장한다.

중요 정책:

- 방문 횟수를 고객 테이블에 중복 저장하지 않는다.
- 스탬프 수를 고객 테이블에 중복 저장하지 않는다.
- 최근 방문일을 고객 테이블에 중복 저장하지 않는다.

조회 시 계산한다.

```text
visitCount    = COUNT(visits.id)
stampCount    = COUNT(visit_stamps.id)
lastVisitedAt = MAX(visits.visited_at)
```

`phone_number`는 중복을 허용하지 않는다.

`qr_token`은 중복을 허용하지 않는다.

---

## 9. CA 및 매장 규칙

`client_advisors`는 `employee_accounts`와 1:1로 연결한다.

CA는 `store_id`를 통해 소속 매장과 연결한다.

CA 업무 API에서는 인증 토큰의 `caId`, `storeId`를 기준으로 접근 권한을 검증한다.

Request Body에서 전달받은 CA ID만 신뢰하여 다른 CA처럼 동작하게 하지 않는다.

다른 매장 데이터 접근은 MVP 정책에 따라 제한한다.

---

## 10. 상품 규칙

상품은 `products`로 관리한다.

핵심 필드:

- `product_code`
- `name`
- `category`
- `image_url`
- `price`
- `dpp_id`
- `is_recommendable`

구매 이력이나 관심 제품에서 참조 중인 상품은 무조건 물리 삭제하지 않는다.

참조 중 상품 삭제 요청은 `PRODUCT_IN_USE` 정책을 따른다.

매장별 재고 관리는 MVP 범위에서 제외한다.

---

## 11. 관심 제품 규칙

테이블:

```text
customer_interest_products
```

출처 Enum:

```text
CUSTOMER
CA
```

### CUSTOMER 저장

```text
source_type = CUSTOMER
visit_record_id = NULL
```

### CA 저장

```text
source_type = CA
visit_record_id != NULL
```

Service에서 위 조합을 반드시 검증한다.

중복 기준:

```text
customer_id + product_id + source_type
```

동일 조합은 중복 저장하지 않는다.

같은 고객이 동일 상품을 `CUSTOMER`, `CA` 두 출처로 각각 저장하는 것은 허용한다.

---

## 12. 방문 규칙

테이블:

```text
visits
```

방문 1회당 1행을 생성한다.

같은 고객이 같은 날 여러 번 방문할 수 있으므로 날짜만 기준으로 Unique 제약을 두지 않는다.

방문 생성 시 존재하는 고객과 매장인지 검증한다.

방문 횟수와 최근 방문일은 `visits` 이력에서 계산한다.

---

## 13. 방문 기록 규칙

테이블:

```text
visit_records
```

방문 기록은 상담 채팅 전문 저장 기능이 아니다.

CA가 방문 후 다음과 같은 고객 맥락을 간단히 기록하는 용도이다.

- 방문 목적
- 고객 반응/내용
- 스타일 변화
- 응대 시 주의사항

정합성:

- 하나의 방문에 방문 기록은 하나만 허용한다.
- `visit_record.customer_id`는 해당 `visit.customer_id`와 일치해야 한다.
- `visit_record.ca_id`는 인증된 CA의 ID와 일치해야 한다.
- 존재하지 않는 `visit_id`는 허용하지 않는다.

---

## 14. 구매 이력 규칙

테이블:

```text
purchase_history
```

MVP에서는 생성/조회 중심으로 구현한다.

`visit_id`가 존재하는 경우 반드시 다음을 검증한다.

```text
purchase.customer_id == visit.customer_id
purchase.store_id == visit.store_id
```

`quantity`는 1 이상이어야 한다.

일반 사용자용 임의 수정/삭제 API를 추가하지 않는다.

---

## 15. 방문 스탬프 규칙

테이블:

```text
visit_stamps
```

기본 정책:

- 하나의 방문에는 스탬프 1개만 발급한다.
- 동일 `visit_id`의 중복 발급을 허용하지 않는다.
- `visit_stamp.customer_id`는 방문 고객과 일치해야 한다.
- `issued_by_ca_id`는 인증된 CA를 기준으로 검증한다.

스탬프 수는 `COUNT(visit_stamps.id)`로 계산한다.

---

## 16. AI Journey Brief 규칙

테이블:

```text
ai_journey_briefs
```

AI 브리프는 **실시간 채팅 기능이 아니라 특정 시점의 고객 여정을 요약한 스냅샷**이다.

MVP에서는 Google Gemini API를 이용해 브리프를 생성한다.

### Gemini 입력

Entity 전체를 Gemini에 전달하지 않고 AI 전용 내부 DTO를 사용한다.

고객:

```text
membershipGrade
stylePreferences
```

과거 방문 기록:

```text
visitedAt
visitPurpose
content
styleChangeNote
cautionNote
```

관심 제품:

```text
productName
category
sourceType
memo
savedAt
```

구매 이력:

```text
productName
category
quantity
purchasedAt
```

### 입력 범위

- 과거 방문 기록은 기준 방문의 `visitedAt` 이전 최신 최대 5개
- 관심 제품은 기준 방문의 `visitedAt` 이전에 저장된 항목 중 최신 최대 10개
- 구매 이력은 기준 방문의 `visitedAt` 이전에 발생한 항목 중 최신 최대 10개
- 과거 기준 방문을 다시 생성할 때 기준 방문 이후 데이터가 섞이지 않도록 시점을 제한한다.

### AI 입력 금지

다음 값은 Gemini 입력에 포함하지 않는다.

```text
name
phoneNumber
loginId
passwordHash
qrToken
customerNo
customerAccountId
employeeAccountId
JWT
GOOGLE_API_KEY
profileImageUrl
```

해커톤 무료 등급에서는 실제 고객 개인정보를 사용하지 않고 더미 데이터만 사용한다.

### 백엔드 직접 계산

```text
visitCount
stampCount
lastVisitedAt
sourceVisitCount
```

위 값은 Gemini에게 계산시키지 않는다.

`sourceVisitCount`는 실제 Gemini 입력에 사용한 과거 방문 기록 수로 계산한다.

### Gemini 출력

JSON Schema 기반 Structured Output을 우선 사용하여 다음 구조로 받는다.

```text
summary
visitPurposeSummary
interestSummary
cautionSummary
suggestedDirection
```

외부 AI 응답은 Structured Output을 사용하더라도 백엔드에서 다시 검증한다.

### DB 상태

```text
GENERATED
FAILED
```

다른 상태값을 임의 추가하지 않는다.

동일 방문 기준 여러 번 브리프 생성은 허용한다.

최신 조회는 `customer_id + visit_id` 기준 `generated_at`이 가장 최신인 행을 반환한다.

Gemini API 실패 시 `FAILED` 브리프 저장을 허용하며 요약 필드는 NULL일 수 있다.

조회 API에서는 Gemini API를 재호출하지 않는다.
## 17. Gemini API 연동 규칙

Gemini 호출 코드는 `ai.client`로 분리한다.

해커톤 기본 권장 모델은 다음과 같다.

```text
gemini-3.6-flash
```

모델명은 Service 여러 곳에 직접 하드코딩하지 않고 설정값으로 관리한다. 사용 전 Google 공식 문서에서 모델의 현재 제공 상태와 Free Tier 지원 여부를 확인한다.

Java에서는 Google 공식 GenAI SDK(`com.google.genai:google-genai`) 사용을 우선한다. 라이브러리 버전은 구현 시점의 최신 안정 버전을 확인한다.

외부 AI 응답을 그대로 신뢰하지 않는다.

반드시:

- JSON Schema 기반 Structured Output 우선 적용
- 응답 구조 검증
- JSON 파싱
- timeout 처리
- 파싱 실패 처리
- 예외 변환

을 수행한다.

대표 오류:

- `AI_API_TIMEOUT`
- `AI_RESPONSE_PARSE_FAILED`

ErrorCode는 특정 AI 공급자 이름을 사용하지 않는다.

API Key는 `GOOGLE_API_KEY` 환경변수로 관리하고 코드에 하드코딩하지 않는다.

외부 API 호출 전체를 장시간 DB 트랜잭션으로 묶지 않는다.

권장 흐름:

```text
필요 데이터 조회
→ AI 전용 Source DTO 구성
→ Gemini API 호출
→ 응답 검증/파싱
→ 짧은 저장 트랜잭션
→ ai_journey_briefs 저장
```

AI 브리프 생성 POST 요청에서만 Gemini API를 호출한다.

최신 브리프 및 브리프 이력 GET 요청에서는 DB만 조회한다.

개발 중 일반 Service 테스트에서는 Gemini 실제 호출을 반복하지 않고 Mock 또는 Stub을 우선 사용한다.
## 18. 트랜잭션 규칙

다음 기능은 비즈니스 단위의 정합성을 보장해야 한다.

- 고객 회원가입
- 방문 기록 생성
- 관심 제품 저장
- 구매 이력 생성
- 스탬프 발급
- AI 브리프 결과 저장

필요한 Service 메서드에 `@Transactional`을 적용한다.

외부 API 통신 때문에 트랜잭션 시간이 불필요하게 길어지지 않도록 한다.

---

## 19. JPA 규칙

- 연관관계를 무조건 양방향으로 만들지 않는다.
- 필요한 방향만 우선 매핑한다.
- `EAGER`를 습관적으로 사용하지 않는다.
- N+1 문제를 확인한다.
- 필요한 경우 Fetch Join, EntityGraph, 전용 조회 Query를 사용한다.
- DBML의 실제 FK 관계와 다른 연관관계를 임의 추가하지 않는다.
- DB 제약만 믿지 말고 Service에서도 비즈니스 정합성을 검증한다.

---

## 20. 공통 에러 코드

현재 프로젝트에서 정의한 에러 코드를 우선 재사용한다.

```text
INVALID_REQUEST
INVALID_CREDENTIALS
TOKEN_EXPIRED
INVALID_TOKEN

FORBIDDEN
FORBIDDEN_CA

ACCOUNT_NOT_FOUND
CUSTOMER_NOT_FOUND
CA_NOT_FOUND
STORE_NOT_FOUND
PRODUCT_NOT_FOUND
VISIT_NOT_FOUND
VISIT_RECORD_NOT_FOUND

DUPLICATE_LOGIN_ID
DUPLICATE_PHONE_NUMBER
DUPLICATE_QR_TOKEN
DUPLICATE_INTEREST_PRODUCT
VISIT_RECORD_ALREADY_EXISTS
STAMP_ALREADY_ISSUED

PRODUCT_IN_USE

INVALID_INTEREST_SOURCE
VISIT_CUSTOMER_MISMATCH

AI_API_TIMEOUT
AI_RESPONSE_PARSE_FAILED
```

새 ErrorCode가 필요한 경우 기존 코드와 의미가 중복되는지 먼저 확인한다.

---

## 21. MVP 제외 기능

다음 기능은 별도 결정 없이 구현하지 않는다.

- 고객과 AI의 실시간 채팅
- 상담 대화 전문 저장
- 결제 연동
- 푸시 알림
- 관리자 백오피스 고도화
- Refresh Token DB 저장
- 매장별 재고 관리
- 상품 추천 알고리즘 고도화
- 파일 업로드 서버 구축
- 공개 직원 회원가입

---

## 22. 필수 테스트

### 인증/인가

- 고객 회원가입
- 고객 로그인 성공/실패
- 직원 로그인 성공/실패
- JWT 만료/위조
- CUSTOMER의 CA API 접근 차단
- 인증되지 않은 보호 API 접근 차단

### 고객

- 로그인 ID 중복
- 전화번호 중복
- 방문 횟수 계산
- 스탬프 수 계산
- 최근 방문일 계산

### 방문/기록

- 방문 생성
- 존재하지 않는 고객/매장
- 방문 기록 중복 생성
- 방문 고객 불일치
- 인증 CA 불일치

### 관심 제품

- CUSTOMER → `visit_record_id = NULL`
- CA → `visit_record_id != NULL`
- 중복 저장 방지
- 잘못된 출처 조합 차단

### 구매/스탬프

- 방문-고객-매장 정합성
- 스탬프 중복 발급 방지
- 방문 고객 불일치

### AI

- 브리프 생성 성공
- Structured Output 5개 필드 검증
- 최신 브리프 조회 시 Gemini 재호출 없음
- 브리프 이력 조회 시 Gemini 재호출 없음
- 기준 방문 이후 데이터가 AI 입력에 섞이지 않는지 검증
- timeout 처리
- JSON 파싱/구조 검증 실패
- FAILED 저장
- 일반 단위 테스트에서는 Mock/Stub 사용

---

## 23. 구현 전 체크리스트

- [ ] `PROJECT.md` 범위와 일치하는가?
- [ ] `MCM_ERD_v6.dbml`의 테이블/컬럼/FK/Enum과 일치하는가?
- [ ] CUSTOMER와 CA 권한을 구분했는가?
- [ ] Request/Response DTO를 사용하는가?
- [ ] Controller에서 Entity를 직접 반환하지 않는가?
- [ ] 비즈니스 로직이 Service에 있는가?
- [ ] Service에서 권한과 데이터 정합성을 검증하는가?
- [ ] MVP 제외 기능을 임의로 추가하지 않았는가?
- [ ] Secret이 코드나 Git에 포함되지 않는가?
- [ ] 필요한 테스트를 작성하거나 실행했는가?
- [ ] 문서와 구현 간 충돌을 발견했다면 임의 해석하지 않고 보고했는가?
