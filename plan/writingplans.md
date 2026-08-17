# MCM Private Circle 담당자 B 전체 MVP 구현 계획

## 문서 상태

- 기준일: 2026.08.17
- 작업 브랜치: `partB`
- 담당자 A 기준선 커밋: `76a84483a417a828309f7117875d018759f42b1b`
- 기준선 검증: Gradle 전체 테스트 성공
- 관심상품 정책: 확정, Blocker 아님
- 외부 전제: Phase 9 MySQL 접속정보와 개인정보 없는 Gemini smoke test용 `GOOGLE_API_KEY`
- GitHub Push: 사용자 승인 전 수행하지 않음
- 로컬 `skills/`: Git 추적 해제 및 `/skills/` ignore 적용

## 1. 목표와 완료 정의

담당자 A의 인증, 고객, 직원, 매장, 상품 구현을 재사용하고 방문, 방문 기록, 관심상품, 구매, 스탬프, AI 브리프, Gemini 연동으로 구성된 담당자 B MVP를 완성한다.

완료는 코드 작성만을 뜻하지 않는다. 다음 조건이 모두 충족돼야 한다.

- A 공개 API와 Response DTO 계약을 유지한다.
- B API는 Entity를 직접 반환하지 않고 `ApiResponse<T>`를 사용한다.
- 모든 B 목록 API는 `ApiResponse<PageResponse<T>>`를 사용한다.
- CUSTOMER, CA, CA 매장, 최초 작성자 경계가 Service에서 검증된다.
- ERD의 FK, nullable, Unique와 Entity가 일치한다.
- 외부 AI 호출은 장시간 DB 트랜잭션 밖에서 수행한다.
- 자동 테스트에서 Gemini 실제 호출이 발생하지 않는다.
- H2 전체 회귀가 통과한다.
- MySQL과 실제 Gemini 검증은 외부 전제가 제공된 범위에서 별도 수행한다.
- 일일 로그, 커밋, GitHub 이슈 상태가 실제 결과와 일치한다.

## 2. 판단 우선순위

충돌 시 다음 순서를 사용한다.

1. 사용자의 최신 명시적 확정사항
2. `PROJECT.md`
3. `MCM_ERD_v6.dbml`
4. `backend-rules.md`
5. 현재 코드 패턴

임의로 새로운 설계안을 만들지 않는다. 확정된 관심상품 정책을 다시 Blocker로 되돌리지 않는다.

## 3. 공통 아키텍처 원칙

### 3.1 수직 슬라이스

각 도메인은 다음 순서로 구현한다.

```text
실패 테스트
-> Entity
-> Repository
-> Request/Response DTO
-> Service
-> Controller
-> 대상 테스트
-> 전체 회귀
```

### 3.2 A 코드 보호

- A 코드를 재구현하거나 대규모 리팩터링하지 않는다.
- A Entity에 B 컬렉션을 추가하지 않는다.
- A Service가 여러 B Repository를 직접 주입받지 않는다.
- 실제 계약 불일치가 발견된 지점만 최소 수정한다.
- A의 공개 Endpoint와 기존 Response DTO 구조를 유지한다.

### 3.3 Entity

- 단방향, LAZY 관계를 기본으로 한다.
- 근거 없는 cascade와 orphanRemoval을 사용하지 않는다.
- public setter를 두지 않는다.
- 필수 FK와 시간은 NOT NULL로 매핑한다.
- Unique 제약 이름을 명시한다.
- `LocalDateTime` 자체에 시간대가 저장된다고 표현하지 않는다.
- 시간 생성과 해석은 `Asia/Seoul` 기준 공통 `Clock`으로 통일한다.

### 3.4 API와 페이지

생성 API는 201, 조회·수정·삭제는 200을 사용한다.

`PageResponse<T>` 필드는 다음으로 고정한다.

```text
items
page
size
totalElements
totalPages
hasNext
```

페이지 입력은 `page >= 0`, `1 <= size <= 100`이며 기본값은 `page=0`, `size=20`이다. 위반 시 `INVALID_REQUEST(400)`를 반환한다.

### 3.5 예외와 DB 충돌

- 예상하지 못한 내부 예외는 `INTERNAL_SERVER_ERROR(500)`의 고정 메시지로 응답한다.
- 실제 예외는 서버 로그에만 기록한다.
- 사전 exists 검사와 DB Unique를 함께 사용한다.
- 알려진 제약명만 도메인 409로 변환한다.
- 알 수 없는 DB 오류는 중복 오류로 바꾸지 않는다.

알려진 제약명:

```text
uk_visit_records_visit
uk_interest_visit_record_product
uk_visit_stamps_visit
```

## 4. 확정 도메인 정책

### 4.1 Visit

- 생성 주체는 CA다.
- Request는 `customerId`, `visitedAt`만 받는다.
- 매장은 `AuthenticatedUser.storeId`에서 파생한다.
- 같은 고객의 같은 날 복수 방문을 허용한다.
- 상세와 고객별 목록은 CA 자기 매장 범위로 제한한다.
- 목록은 `visitedAt DESC, id DESC`로 정렬한다.

### 4.2 VisitRecord

- 고객과 CA는 Visit과 Principal에서 파생한다.
- 방문당 하나만 허용한다.
- Visit 고객과 VisitRecord 고객은 일치해야 한다.
- Visit 매장과 Principal 매장은 일치해야 한다.
- 수정은 최초 작성 CA만 허용한다.
- PATCH는 최소 한 필드가 존재해야 한다.

### 4.3 관심상품

공통 필드:

```text
id
customer
product
sourceType
visitRecord
memo
savedAt
```

CUSTOMER 저장:

- 고객은 Principal의 `customerId`
- `sourceType=CUSTOMER`
- `visitRecord=null`
- 중복은 고객+상품 Service 사전 검사
- Partial Unique와 Generated Column은 사용하지 않음
- 극단적인 동시 요청은 Known Limitation

CA 저장:

- 고객은 Path의 `customerId`
- `sourceType=CA`
- `visitRecord=request.visitRecordId`
- 연결 VisitRecord의 고객, 매장, 작성자 검증
- 중복은 `UNIQUE(visit_record_id, product_id)`
- 다른 VisitRecord에서 같은 상품 저장 허용

조회와 삭제:

- CUSTOMER는 본인의 CUSTOMER 항목만 조회한다.
- CA는 CUSTOMER 공통 항목과 자기 매장의 CA 항목을 DB 단계에서 하나의 정렬 결과로 만든다.
- 정렬은 `savedAt DESC, id DESC`다.
- CA 출처 삭제는 연결 VisitRecord의 최초 작성 CA만 허용한다.
- 타 매장 CA 데이터 존재를 조회, 저장 오류, 삭제 오류로 노출하지 않는다.

### 4.4 PurchaseHistory

- Request는 고객, 상품, 선택 Visit, 수량, 구매 시각을 받는다.
- 매장은 Principal에서 파생한다.
- Visit이 있으면 고객과 매장이 구매 정보와 일치해야 한다.
- 수량은 1 이상이다.
- MVP는 생성과 CA 자기 매장 고객별 조회만 제공한다.

### 4.5 VisitStamp

- 고객은 Visit, 발급 CA는 Principal에서 파생한다.
- 방문당 하나만 허용한다.
- `stampType`은 필수, 최대 30자 String이다.
- CA는 자기 매장 기록, CUSTOMER는 본인 전체 기록을 조회한다.
- 정렬은 `issuedAt DESC, id DESC`다.

## 5. A-B 최소 조회 계약

### 5.1 상품 참조

```text
ProductService
-> ProductReferenceChecker
-> Interest/Purchase 참조 여부
```

`ProductService.deleteProduct()`는 포트의 boolean 결과만 사용하고 기존 CRUD 구조를 유지한다. 참조 중이면 `PRODUCT_IN_USE(409)`를 반환한다.

### 5.2 고객 활동 집계

```text
CustomerService
-> CustomerActivitySummaryReader
-> visitCount, stampCount, lastVisitedAt
```

실제 데이터 0건은 `0, 0, null`이다. DB 장애는 숨기지 않고 전파한다.

### 5.3 JWT

- `ExpiredJwtException`은 `TOKEN_EXPIRED`
- 위조, Malformed, 필수 Claim 누락, Claim 형식, Role 오류는 `INVALID_TOKEN`
- Filter가 예외를 request attribute로 전달하고 AuthenticationEntryPoint를 직접 호출한다.
- Filter Chain은 오류 응답 후 종료한다.

## 6. AI 브리프 설계

### 6.1 Endpoint

```http
POST /api/v1/customers/{customerId}/ai-briefs
GET /api/v1/customers/{customerId}/ai-briefs/latest?visitId={visitId}
GET /api/v1/customers/{customerId}/ai-briefs?page=0&size=20
```

latest는 GENERATED/FAILED를 포함한 가장 최근 생성 시도이며 `generatedAt DESC, id DESC`로 결정한다. 최신 행이 FAILED여도 200으로 반환한다. customer+visit 기준 행 자체가 없을 때만 `AI_BRIEF_NOT_FOUND(404)`다.

### 6.2 입력 범위

- 고객 공통: membershipGrade, stylePreferences
- VisitRecord: 현재 CA 매장, 기준 방문보다 이전, 최신 5건
- CUSTOMER 관심상품: 기준 방문보다 이전, 매장 제한 없음
- CA 관심상품: savedAt과 연결 Visit 시각이 모두 기준 방문보다 이전이고 현재 CA 매장인 항목
- 관심상품 통합: savedAt DESC, id DESC, 최대 10건
- Purchase: 현재 CA 매장, 기준 방문보다 이전, 최신 10건
- sourceVisitCount: 실제 포함한 VisitRecord 수

Gemini 입력에서 이름, 전화번호, 로그인 ID, 비밀번호 해시, QR, 고객번호, 계정 ID, JWT, API Key, 프로필 이미지를 제외한다.

### 6.3 출력

Structured Output은 다음 다섯 필드만 허용한다.

```text
summary
visitPurposeSummary
interestSummary
cautionSummary
suggestedDirection
```

역직렬화 후 누락, null, blank, 예상 외 필드를 재검증한다.

### 6.4 트랜잭션과 실패

```text
4xx 사전 검증 실패
-> Gemini 미호출
-> FAILED 미저장

Gemini 성공
-> 응답 검증
-> REQUIRES_NEW 짧은 트랜잭션
-> GENERATED 저장

외부 호출·응답 처리 실패
-> REQUIRES_NEW 짧은 트랜잭션
-> FAILED 저장 커밋
-> 502 발생
```

Orchestrator는 장시간 트랜잭션을 사용하지 않는다.

### 6.5 Gemini 설정

- SDK: `com.google.genai:google-genai:1.66.0`
- 모델 기본값: `gemini-3.6-flash`
- 요청 timeout: `PT30S`
- API Key: `GOOGLE_API_KEY`
- connect/read timeout 분리는 필요가 확인될 때만 커스텀 HTTP Client로 확장한다.

## 7. Phase별 실행 목록

| Phase | GitHub 이슈 | 핵심 작업 | 종료 Gate |
|---:|---|---|---|
| 0 | [#1](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/1) | A 기준선, 공통 계약, ERD, 계획, 로그 | A+공통 전체 테스트 |
| 1 | [#2](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/2) | Visit | Service/Controller/전체 테스트 |
| 2 | [#3](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/3) | VisitRecord | Unique/작성자/전체 테스트 |
| 3 | [#4](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/4) | Interest | 중복/격리/통합 페이지 테스트 |
| 4 | [#5](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/5) | Purchase | 방문 정합성/전체 테스트 |
| 5 | [#6](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/6) | Stamp | 방문 Unique/역할 조회 테스트 |
| 6 | [#7](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/7) | A-B 포트, JWT, 500 | A 회귀와 HTTP 보안 테스트 |
| 7 | [#8](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/8) | AI Entity, Source, GET | 시점·매장 격리, Client 0회 |
| 8 | [#9](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/9) | Gemini POST | 성공/FAILED 독립 커밋 테스트 |
| 9 | [#10](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/10) | H2, MySQL, Gemini 검증 | 가능한 외부 검증과 미완료 기록 |
| 10 | [#11](https://github.com/chun9930/Hackathon_gaebal-saebal-BE/issues/11) | 최종 리뷰, 문서, 로그 | 전체 테스트와 교차 검증 |

## 8. 파일 단위 구현 목록

### Phase 0 공통

- `global/config/TimeConfig.java`
- `global/response/PageResponse.java`
- `global/util/PaginationValidator.java`
- `global/exception/ConstraintNameResolver.java`
- `global/exception/ErrorCode.java`
- `global/exception/GlobalExceptionHandler.java`
- `src/test/resources/application-test.yaml`
- `src/main/resources/application-mysql-verification.yaml`
- `MCM_ERD_v6.dbml`

### Phase 1~5 B 도메인

- `visit/entity/Visit.java`
- `visit/entity/VisitRecord.java`
- `visit/repository/*`
- `visit/dto/*`
- `visit/service/*`
- `visit/controller/*`
- `interest/entity/*`
- `interest/repository/*`
- `interest/dto/*`
- `interest/service/*`
- `interest/controller/*`
- `purchase/entity/*`
- `purchase/repository/*`
- `purchase/dto/*`
- `purchase/service/*`
- `purchase/controller/*`
- `stamp/entity/*`
- `stamp/repository/*`
- `stamp/dto/*`
- `stamp/service/*`
- `stamp/controller/*`

### Phase 6 A-B 연동

- `product/service/ProductReferenceChecker.java`
- `product/service/DomainProductReferenceChecker.java`
- `product/service/ProductService.java`
- `customer/dto/CustomerActivitySummary.java`
- `customer/service/CustomerActivitySummaryReader.java`
- `customer/service/DomainCustomerActivitySummaryReader.java`
- `customer/service/CustomerService.java`
- JWT Provider, Filter, EntryPoint와 관련 테스트

### Phase 7~8 AI

- `ai/entity/BriefStatus.java`
- `ai/entity/AiJourneyBrief.java`
- `ai/repository/AiJourneyBriefRepository.java`
- `ai/dto/AiBriefCreateRequest.java`
- `ai/dto/AiBriefResponse.java`
- `ai/dto/AiBriefSource.java`
- `ai/dto/GeminiBriefResult.java`
- `ai/service/AiBriefSourceReader.java`
- `ai/service/AiBriefPersistenceService.java`
- `ai/service/AiBriefService.java`
- `ai/client/GeminiBriefClient.java`
- `ai/client/GoogleGeminiBriefClient.java`
- `ai/client/AiClientTimeoutException.java`
- `ai/client/AiClientException.java`
- `ai/config/GeminiProperties.java`
- `ai/config/GeminiConfig.java`
- `ai/controller/AiBriefController.java`

## 9. 테스트 Gate

각 Phase는 다음 순서를 따른다.

1. 요구 동작을 표현하는 실패 테스트 추가
2. 예상 원인으로 실패하는지 확인
3. 최소 구현
4. 대상 테스트 성공
5. 전체 `.\gradlew.bat test` 성공
6. 코드, 테스트, ERD, API 계약 교차 검증
7. 일일 로그 갱신
8. Phase 커밋

필수 회귀에는 매장 파생, 작성자 제한, 관심상품 두 중복 기준, 페이지 경계, JWT HTTP 응답, 내부 메시지 미노출, AI 미래·타 매장 제외, 4xx FAILED 미저장, FAILED 저장 후 502, GET Client 0회가 포함된다.

## 10. MySQL과 인덱스 Gate

다음 인덱스는 후보일 뿐이며 코드 작성 단계에서 확정하지 않는다.

```text
visits(customer_id, store_id, visited_at, id)
customer_interest_products(customer_id, source_type, saved_at, id)
purchase_history(customer_id, store_id, purchased_at, id)
visit_stamps(customer_id, issued_at, id)
ai_journey_briefs(customer_id, visit_id, generated_at, id)
ai_journey_briefs(customer_id, generated_at, id)
```

Repository Query 완성 후 Hibernate SQL을 수집하고 MySQL 대표 데이터에서 `EXPLAIN`한 결과로 최소 복합 인덱스만 반영한다.

## 11. Git과 로그

- 커밋은 Conventional Commit 형식과 한국어 명사·명령조를 사용한다.
- 본문은 변경 이유를 `Why:`로 기록한다.
- Phase 중간 커밋은 `Refs #번호`, Phase 마지막 커밋은 `Closes #번호`를 사용한다.
- Push는 사용자 승인 후 수행한다.
- 변경이 있는 날만 `daily-log/YYYY.MM.DD.md`를 만든다.
- 완료 항목은 코드, 테스트, 문서, Git 상태를 교차 검증한 뒤 수정한다.

## 12. Known Limitation과 외부 전제

- CUSTOMER 관심상품은 DB partial unique 없이 Service 중복 검사만 사용하므로 극단적인 동시 요청 경쟁이 남는다.
- MySQL 접속정보가 없으면 Phase 9 MySQL 실검증을 완료로 표시하지 않는다.
- `GOOGLE_API_KEY`가 없으면 실제 Gemini smoke test를 완료로 표시하지 않는다.
- 외부 검증 미완료는 코드 구현 완료와 구분해 일일 로그와 최종 보고에 남긴다.
