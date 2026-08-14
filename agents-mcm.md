# MCM Private Circle Agents

## 1. 목적

이 문서는 MCM Private Circle 백엔드 프로젝트에서 AI Agent가 기능 설계, 구현, ERD 검토, QA, 리뷰를 수행할 때의 역할과 실행 기준을 정의한다.

Agent의 최우선 목표는 다음과 같다.

- MVP 범위 준수
- CUSTOMER / CA 권한 분리
- ERD와 코드의 일치
- 데이터 정합성 보장
- API 계약 일관성 유지
- 구현 후 검증
- 불필요한 기능 확장 방지

---

## 2. 공통 기준

Agent는 작업에 필요한 범위에서 다음 자료를 먼저 확인한다.

```text
PROJECT.md
MCM_ERD_v6.dbml
backend-rules.md
현재 코드
현재 API/기능 문서
사용자의 최신 확정사항
```

판단 우선순위:

```text
1. 사용자의 최신 명시적 결정사항
2. PROJECT.md의 MVP 및 API 정책
3. MCM_ERD_v6.dbml의 실제 테이블/컬럼/FK/Enum
4. backend-rules.md
5. 현재 코드 패턴
```

서로 충돌하면 임의로 한쪽을 선택하지 않고 충돌 내용을 보고한다.

---

## 3. 공통 실행 원칙

모든 Agent는 다음을 지킨다.

- 실제 파일을 확인한 뒤 작업한다.
- 요구사항을 MVP 포함/제외 관점에서 확인한다.
- CUSTOMER와 CA를 구분한다.
- DTO를 사용한다.
- Entity를 Controller 응답으로 직접 반환하지 않는다.
- Service에서 권한을 최종 검증한다.
- Service에서 FK 및 도메인 정합성을 검증한다.
- ERD에 없는 기능이나 상태값을 임의 추가하지 않는다.
- 기존 공통 응답/예외 구조가 있으면 재사용한다.
- Secret을 노출하지 않는다.
- 구현 후 테스트 또는 검증을 수행한다.
- 추측한 내용은 확정사항처럼 구현하지 않는다.

---

# 4. Agent 역할

## 4.1 요구사항 분석 Agent

### 책임

- 사용자 요청 분석
- MVP 포함 여부 판단
- 기능 주체 판별
- 관련 테이블 식별
- 기존 정책과 충돌 탐지
- 구현 전에 필요한 미결정 사항 발견

### 기능 주체

반드시 다음 중 하나 이상으로 구분한다.

```text
Public
CUSTOMER
CA
```

### 확인 항목

- 누가 사용하는 기능인가?
- 인증이 필요한가?
- 어떤 Entity/Table을 사용하는가?
- 생성/조회/수정/삭제 중 어떤 작업인가?
- 현재 API가 이미 존재하는가?
- MVP에 포함되어 있는가?
- ERD 변경이 필요한가?
- 기존 도메인으로 구현 가능한가?

명확한 기존 기능 구현이나 버그 수정에는 불필요한 기획 문서를 강제하지 않는다.

새로운 기능이거나 범위가 불명확하면 구현 전 요구사항을 먼저 정리한다.

---

## 4.2 Backend Agent

### 책임

- REST API 설계
- Request / Response DTO 작성
- Entity 및 Repository 구현
- Service 비즈니스 로직 구현
- Controller 구현
- 인증/인가 적용
- 예외 처리
- 트랜잭션 처리
- 테스트 작성

### 기본 실행 순서

```text
요구사항 확인
→ 관련 PROJECT 정책 확인
→ 관련 ERD 확인
→ 기존 코드 확인
→ API 계약 확인
→ 권한/정합성 규칙 정의
→ DTO 설계
→ Service 구현
→ Repository 구현
→ Controller 연결
→ 테스트
→ 리뷰
```

### 필수 규칙

- Controller → Service → Repository
- Entity 직접 반환 금지
- DTO 필수
- Controller 비즈니스 로직 금지
- Service 권한 검증 필수
- FK 정합성 검증
- 중복 정책 검증
- 공통 ErrorCode 재사용
- Secret 하드코딩 금지

---

## 4.3 Auth / Security Agent

### 책임

- Spring Security 설정
- JWT 발급/검증
- CUSTOMER / CA 인증 객체 설계
- Endpoint 접근 제어
- Service 권한 검증
- BCrypt 처리
- Secret 관리 검토

### 계정 구조

고객:

```text
customer_accounts ↔ customers
ROLE_CUSTOMER
```

CA:

```text
employee_accounts ↔ client_advisors
ROLE_CA
```

### 고객 JWT

```text
accountId
customerId
role
```

### CA JWT

```text
accountId
caId
storeId
role
```

### 반드시 차단

- CUSTOMER의 CA 업무 API 접근
- 공개 직원 회원가입
- Request Body의 `caId`만 신뢰하여 다른 CA처럼 동작
- 타 매장 데이터에 대한 무권한 접근
- Secret 하드코딩
- 평문 비밀번호 저장
- 비밀번호/해시 로그 출력

---

## 4.4 Customer / Account Agent

### 책임

- 고객 회원가입
- 고객 로그인
- 고객 프로필 조회/수정
- QR Token 정책
- 고객 계산값 조회

### 필수 규칙

회원가입 시:

```text
customer_accounts
+
customers
```

를 함께 생성한다.

QR Token은 서버에서 생성한다.

다음 값은 고객 테이블에 중복 저장하지 않는다.

```text
방문 횟수
스탬프 수
최근 방문일
```

조회 시 계산한다.

---

## 4.5 Store / Product Agent

### 책임

- 매장 목록/상세 조회
- 상품 목록/상세 조회
- CA 상품 등록/수정/삭제
- 상품 참조 상태 검증

### 상품 삭제 검토

삭제 전에 다음 참조를 확인한다.

```text
customer_interest_products
purchase_history
```

참조 중인 상품은 무조건 물리 삭제하지 않는다.

매장별 재고 관리 기능은 추가하지 않는다.

---

## 4.6 Visit Agent

### 책임

- 방문 생성
- 방문 상세 조회
- 고객별 방문 이력 조회
- 방문 기록 생성/조회/수정
- 방문 관련 정합성 검증

### 방문

```text
visits
```

동일 고객이 같은 날 여러 번 방문할 수 있다.

날짜만 기준으로 중복 방문을 막지 않는다.

### 방문 기록

```text
visit_records
```

하나의 방문에 방문 기록은 하나만 허용한다.

검증:

```text
visit 존재
visit.customer_id == visit_record.customer_id
authenticatedCaId == visit_record.ca_id
```

방문 기록을 상담 대화 전문 저장 용도로 사용하지 않는다.

---

## 4.7 Interest Agent

### 책임

- 고객 관심 제품 저장/조회/삭제
- CA 관심 제품 저장/조회
- 출처 검증
- 방문 기록 연결 검증
- 중복 검증

### Enum

```text
CUSTOMER
CA
```

### CUSTOMER

```text
source_type = CUSTOMER
visit_record_id = NULL
```

### CA

```text
source_type = CA
visit_record_id != NULL
```

### 중복 기준

```text
customer_id + product_id + source_type
```

---

## 4.8 Purchase / Stamp Agent

### Purchase

책임:

- 구매 이력 생성
- 고객 구매 이력 조회
- visit 연결 시 정합성 검증

검증:

```text
purchase.customer_id == visit.customer_id
purchase.store_id == visit.store_id
quantity >= 1
```

### Stamp

책임:

- 방문 스탬프 발급
- 고객/내 스탬프 조회
- 중복 발급 방지

검증:

```text
visit_id 중복 발급 금지
stamp.customer_id == visit.customer_id
issued_by_ca_id == authenticatedCaId
```

---

## 4.9 AI Brief Agent

### 책임

- AI 브리프 입력 데이터 수집
- OpenAI Prompt 구성
- OpenAI Client 호출
- JSON 응답 파싱
- AI 실패 처리
- `ai_journey_briefs` 저장
- 최신 브리프 조회

### AI 기능 목적

AI는 고객과 실시간 대화하지 않는다.

실행 흐름:

```text
고객 데이터 조회
→ 과거 방문 기록 조회
→ 관심 제품 조회
→ 구매 이력 조회
→ 주의사항 조회
→ OpenAI 요청
→ 응답 파싱
→ AI Journey Brief 저장
```

### 출력

```text
summary
visitPurposeSummary
interestSummary
cautionSummary
suggestedDirection
```

### 상태

```text
GENERATED
FAILED
```

다른 상태값을 임의 추가하지 않는다.

### 금지

- 고객 ↔ AI 실시간 채팅
- 상담 대화 전문 저장
- AI 응답을 검증 없이 DB에 반영
- OpenAI Key 하드코딩
- 장시간 외부 API 호출을 DB 트랜잭션 전체와 묶기

---

## 4.10 ERD Review Agent

### 책임

- DBML과 코드 일치 여부 검토
- PK/FK/Unique/Nullable 검토
- Entity 연관관계 검토
- 도메인 정합성 검토
- 불필요한 테이블/컬럼 추가 탐지

### 핵심 관계

반드시 확인한다.

```text
customer_accounts ↔ customers
employee_accounts ↔ client_advisors
stores → client_advisors
customers → visits
stores → visits
visits → visit_records
customers → visit_records
client_advisors → visit_records
customers → customer_interest_products
products → customer_interest_products
visit_records → customer_interest_products
customers → purchase_history
products → purchase_history
stores → purchase_history
visits → purchase_history
visits → visit_stamps
customers → visit_stamps
client_advisors → visit_stamps
customers → ai_journey_briefs
visits → ai_journey_briefs
client_advisors → ai_journey_briefs
```

### 중요 검토 사항

- 계정과 프로필 1:1 관계
- 관심 제품 source_type 정책
- 방문 기록 1개 정책
- 방문/고객 정합성
- 구매/방문/매장 정합성
- 방문 스탬프 중복
- AI Brief 상태 Enum
- ERD에 없는 기능 추가 여부

문서 Note와 실제 컬럼 또는 확정 정책이 서로 맞지 않으면 충돌을 보고한다.

---

## 4.11 QA Agent

### 책임

- 정상 케이스 검증
- 실패 케이스 검증
- Role 경계 검증
- 입력 검증
- FK 정합성 검증
- 중복/충돌 검증
- AI 실패 검증
- 회귀 테스트

### Public

검증:

- 고객 회원가입
- 고객 로그인
- 직원 로그인
- 보호 API 접근 차단

### CUSTOMER

검증:

- 내 프로필
- 내 관심 제품
- 내 스탬프
- CA API 접근 차단
- 타 고객 데이터 접근 차단

### CA

검증:

- 고객 조회
- QR 고객 조회
- 방문 생성
- 방문 기록
- 관심 제품 기록
- 구매 이력
- 스탬프
- AI 브리프
- 인증 CA ID 일치 여부
- 소속 매장 접근 범위

### 필수 Negative Case

```text
존재하지 않는 customerId
존재하지 않는 storeId
존재하지 않는 productId
존재하지 않는 visitId
중복 loginId
중복 phoneNumber
방문 기록 중복 생성
관심 제품 출처 조합 오류
관심 제품 중복
방문 고객 불일치
구매 방문/매장 불일치
스탬프 중복 발급
JWT 만료
JWT 위조
권한 없는 API 접근
AI timeout
AI JSON 파싱 실패
```

---

## 4.12 Reviewer Agent

### 책임

구현이 단순히 실행되는지만 보지 않고 프로젝트 정책과 일치하는지 검토한다.

### Architecture

- DTO 사용 여부
- Entity 직접 반환 여부
- Controller 비즈니스 로직 여부
- Service/Repository 역할 분리

### Security

- CUSTOMER / CA 권한 분리
- Service 권한 검증
- 인증 사용자 대신 Request ID를 신뢰하는 문제
- Secret 노출
- 비밀번호 로그 노출

### Domain

- ERD FK 일치
- 방문-고객 정합성
- 방문 기록-방문-고객-CA 정합성
- 관심 제품 source_type
- 구매-방문-매장 정합성
- 스탬프 중복
- AI Brief 상태 및 기준 방문

### Performance

- N+1
- 불필요한 EAGER
- 반복 Count Query
- 무제한 방문 이력 조회
- AI Prompt에 과도한 데이터 포함

### MVP

- 제외 기능 추가 여부
- Refresh Token DB 구현 여부
- 관리자 기능 임의 확장 여부
- 실시간 AI 채팅 여부
- 재고 관리 임의 추가 여부

### 결과 구분

리뷰 결과는 다음처럼 명확히 분류한다.

```text
문제 없음
수정 권장
반드시 수정
정책 결정 필요
```

---

# 5. 작업 유형별 Workflow

## 5.1 기능 구현

```text
요구사항 확인
→ PROJECT 확인
→ ERD 확인
→ 기존 코드 확인
→ API 계약 확인
→ 권한/정합성 정의
→ 구현
→ 테스트
→ 리뷰
```

---

## 5.2 API 설계

```text
사용 주체
→ Endpoint
→ HTTP Method
→ Request DTO
→ Response DTO
→ 인증/권한
→ Service 검증
→ ErrorCode
→ HTTP Status
```

Entity를 그대로 Response로 설계하지 않는다.

---

## 5.3 ERD / Entity 검토

```text
관련 테이블 확인
→ PK/FK 확인
→ Enum 확인
→ Null/Unique 확인
→ PROJECT 정책 비교
→ Entity 매핑 비교
→ Service 정합성 비교
→ 충돌/누락 보고
```

추가 문제가 없으면 명확히 `추가 문제 없음`이라고 결론낸다.

---

## 5.4 QA / 검증

```text
정상 케이스
→ 인증/권한
→ 입력 검증
→ FK 정합성
→ 중복/충돌
→ AI 실패
→ 회귀 테스트
→ 결과 정리
```

여러 번 검증을 요청받으면 같은 내용을 반복하는 대신 관점을 나누어 확인한다.

예:

```text
1차: 요구사항 및 API 일치
2차: ERD / DB 정합성
3차: 권한 / 예외 / 누락
```

---

## 5.5 버그 수정

```text
재현
→ 원인 코드 확인
→ 관련 도메인 정책 확인
→ 최소 수정
→ 영향 범위 테스트
→ 회귀 검증
```

버그를 고치기 위해 확정된 도메인 정책 자체를 임의 변경하지 않는다.

---

# 6. 문서화 규칙

중요한 설계 또는 QA 산출물이 필요한 경우 다음 구조를 사용할 수 있다.

```text
handoff/
├── pm/
├── eng-review/
└── qa/
```

다음 경우 문서 작성 또는 갱신을 권장한다.

- 새 기능/API 설계
- ERD 변경
- 중요한 정책 확정
- 팀 공유가 필요한 QA 결과
- 큰 구조 변경

단순 코드 수정마다 불필요한 문서 생성을 강제하지 않는다.

프로젝트에서 `daily-log/`를 실제 운영하는 경우에만 의미 있는 작업 내용을 기록한다.

---

# 7. 외부 기술 문서 사용

Spring Boot, Spring Security, JPA, OpenAI API처럼 버전에 따라 구현 방식이 달라질 수 있는 기능을 새로 구현하거나 변경할 때는 공식 문서를 우선한다.

우선 출처:

- Spring 공식 문서
- Spring Security 공식 문서
- Hibernate/JPA 공식 문서
- OpenAI 공식 문서

프로젝트 내부의 단순 리팩터링이나 확정된 로직 수정은 현재 코드와 프로젝트 문서를 우선한다.

---

# 8. 완료 조건

의미 있는 작업은 최소 다음을 확인한 뒤 완료한다.

- [ ] 요구사항을 확인했다.
- [ ] MVP 범위를 확인했다.
- [ ] 관련 ERD를 확인했다.
- [ ] 사용자 Role을 확인했다.
- [ ] DTO를 사용했다.
- [ ] Service 권한 검증이 있다.
- [ ] FK/도메인 정합성을 검증했다.
- [ ] 공통 예외 형식을 지켰다.
- [ ] 필요한 테스트 또는 검증을 수행했다.
- [ ] Secret을 노출하지 않았다.
- [ ] MVP 제외 기능을 임의 추가하지 않았다.
- [ ] 문서/ERD/코드 충돌이 있다면 보고했다.

---

# 9. 최종 실행 원칙

Agent는 항상 다음을 우선한다.

```text
정확성
→ 권한 안전성
→ 데이터 정합성
→ PROJECT / ERD 일치
→ MVP 범위
→ 최소한의 복잡도
```

요구사항이나 문서에서 근거를 찾을 수 없는 내용은 임의로 확정하지 않는다.
