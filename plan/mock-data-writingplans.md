# MCM Private Circle 목업 리소스 최소 구성 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 첨부된 `stores.json`, `products.json`, 상품 이미지 리소스를 기준으로 실제 Spring Boot 로딩까지 가능한 최소 mock 데이터 구성을 정리한다.

**Architecture:** 범위는 매장 데이터 19개, 상품 데이터 19개, 정적 이미지 리소스로 한정한다. mock 데이터는 `src/main/resources/mock-data`에 두고, 이미지는 `src/main/resources/static/*.jpg`에 둔다. `app.mock-data.seed-enabled=true`일 때만 애플리케이션 시작 시 Store/Product 테이블로 적재되도록 구성한다. 다른 직원/고객/방문/구매/스탬프/AI 시나리오 JSON은 이번 범위에서 작성하지 않는다.

**Tech Stack:** JSON, Spring Boot `ApplicationRunner`, Jackson, H2 기반 통합 테스트, Store/Product 엔티티 매핑, Spring static resource serving

---

## 최종 산출물

- `mock-data/stores.json`
- `mock-data/products.json`
- `src/main/resources/mock-data/stores.json`
- `src/main/resources/mock-data/products.json`
- `src/main/resources/static/product-01.jpg` ~ `product-19.jpg`
- mock seed 로직 및 검증 테스트

## 데이터 정책

- stores: 최종 19개
  - 원본 첨부 7개 유지
  - 부족한 12개는 동일 형식의 mock 확장 데이터로 보강
- products: 최종 19개
  - 첨부 원본 19개 기준 유지
- imageUrl: 모두 `/product-XX.jpg` 형식으로 통일
- DPP ID: 현재 Product 엔티티에는 있으나 mock 동작 로직과 조회 검증에 직접 필요하지 않아 이번 JSON에서는 제외
- recommendable: Product 응답 필드와 boolean 매핑 검증에 실제로 필요하므로 유지
- MySQL/Gemini 실검증: 미실행 상태 유지

## 범위 제외

다음 JSON은 이번 계획에서 작성하지 않는다.

- 직원계정
- 직원 프로필
- 고객계정
- 고객
- 방문
- 방문기록
- 관심상품
- 구매이력
- 스탬프
- AI 브리프 요청 시나리오
- manifest 파일

## Task 1: 첨부 원본 대비 stores/products 구조 검증

**Files:**
- Verify: `mock-data/stores.json`
- Verify: `mock-data/products.json`
- Reference: `C:/Users/qudrn/OneDrive/문서/카카오톡 받은 파일/stores.json`
- Reference: `C:/Users/qudrn/OneDrive/문서/카카오톡 받은 파일/products.json`

- [ ] 첨부 원본의 기본 필드 구조를 확인한다.
- [ ] Store 엔티티 기준 `name`, `location`만 유지한다.
- [ ] Product 엔티티 기준 `productCode`, `name`, `category`, `price`, `imageUrl`, `recommendable`만 유지한다.
- [ ] DPP ID는 현재 mock 동작 범위에 불필요한지 검토하고 제외 근거를 문서화한다.

## Task 2: 클래스패스 mock 리소스 19개 정합화

**Files:**
- Modify: `src/main/resources/mock-data/stores.json`
- Modify: `src/main/resources/mock-data/products.json`
- Modify: `mock-data/stores.json`
- Modify: `mock-data/products.json`
- Verify: `src/main/resources/static/*.jpg`

- [ ] stores를 19개로 맞춘다.
- [ ] products를 19개로 맞춘다.
- [ ] products의 모든 `imageUrl`이 실제 정적 이미지와 1:1로 연결되는지 확인한다.
- [ ] UTF-8 JSON으로 정리한다.

## Task 3: mock 데이터 로딩 로직 유지 및 검증

**Files:**
- Verify: `src/main/java/com/mcm/privatecircle/global/config/MockDataSeeder.java`
- Verify: `src/main/java/com/mcm/privatecircle/global/config/MockDataProperties.java`
- Verify: `src/main/resources/application.yaml`
- Verify: `src/test/resources/application-test.yaml`

- [ ] `app.mock-data.seed-enabled=true`일 때만 seed되도록 유지한다.
- [ ] Store/Product 테이블이 비어 있을 때만 적재되는지 검증한다.
- [ ] 기존 StoreService/ProductService가 seed 데이터로 바로 동작 가능한지 테스트로 검증한다.

## Task 4: 테스트 보강

**Files:**
- Modify: `src/test/java/com/mcm/privatecircle/mockdata/MockDataResourceContractTest.java`
- Modify: `src/test/java/com/mcm/privatecircle/mockdata/MockDataSeederIntegrationTest.java`

- [ ] 리소스 파일 개수 검증: stores 19, products 19
- [ ] productCode 유일성 검증
- [ ] imageUrl 실제 정적 리소스 존재 검증
- [ ] seed 활성화 시 DB 적재 검증
- [ ] StoreService/ProductService 조회 로직 동작 검증

## Task 5: 최종 검증

**Files:**
- Verify: 전체 변경 파일

- [ ] JSON 파싱 검증
- [ ] mock-data 관련 테스트 실행
- [ ] 전체 H2 회귀 테스트 실행
- [ ] MySQL/Gemini 실검증은 수행하지 않았음을 명시한다.
