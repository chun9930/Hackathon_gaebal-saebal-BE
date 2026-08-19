# Part A 작업 기록 (2026-08-19 ~ 2026-08-20)

## 작성 범위

- 기준 브랜치: `codex/partA`
- 포함 범위: 2026-08-19 커밋 `0f18925`, `90f0a77`과 2026-08-20 현재 작업 트리의 변경사항
- 초기 프론트 원본을 가져온 커밋은 제외했다.
- 아래 내용은 해당 커밋의 Git diff, 현재 `git diff`, untracked 파일 및 현재 코드를 대조해 작성했다.

## BE

### 수정한 파일

#### 인증, 상품 및 정적 이미지 연동

- `src/main/java/com/mcm/privatecircle/global/security/SecurityConfig.java`
- `src/main/java/com/mcm/privatecircle/product/dto/ProductSummaryResponse.java`
- `src/main/java/com/mcm/privatecircle/product/service/ProductService.java`
- `src/main/resources/mock-data/products.json`
- `src/main/resources/static/images/product/{acc001~003,bag001~010,per001~003,tec001~003}.jpg`

#### 개발용 CA, 매장 및 스탬프

- `src/main/java/com/mcm/privatecircle/employee/entity/ClientAdvisor.java`
- `src/main/java/com/mcm/privatecircle/global/config/MockDataProperties.java`
- `src/main/java/com/mcm/privatecircle/global/config/MockDataSeeder.java`
- `src/main/java/com/mcm/privatecircle/stamp/service/VisitStampService.java`
- `src/main/java/com/mcm/privatecircle/store/repository/StoreRepository.java`
- `src/main/resources/application.yaml`
- `src/main/resources/application-mysql-verification.yaml`
- `src/main/resources/mock-data/stores.json`

#### AI 브리프

- `src/main/java/com/mcm/privatecircle/ai/client/GoogleGeminiBriefClient.java`
- `src/main/java/com/mcm/privatecircle/ai/client/AiClientAuthenticationException.java`
- `src/main/java/com/mcm/privatecircle/ai/client/AiClientConfigurationException.java`
- `src/main/java/com/mcm/privatecircle/ai/config/GeminiProperties.java`
- `src/main/java/com/mcm/privatecircle/ai/controller/AiBriefController.java`
- `src/main/java/com/mcm/privatecircle/ai/dto/AiBriefSource.java`
- `src/main/java/com/mcm/privatecircle/ai/service/AiBriefPersistenceService.java`
- `src/main/java/com/mcm/privatecircle/ai/service/AiBriefService.java`
- `src/main/java/com/mcm/privatecircle/ai/service/AiBriefSourceReader.java`
- `src/main/java/com/mcm/privatecircle/global/exception/ErrorCode.java`
- `src/main/java/com/mcm/privatecircle/global/security/CustomAccessDeniedHandler.java`
- `src/main/java/com/mcm/privatecircle/global/security/CustomAuthenticationEntryPoint.java`
- `src/main/java/com/mcm/privatecircle/global/security/JwtAuthenticationFilter.java`

#### 상담 기록 삭제 및 계약 문서

- `src/main/java/com/mcm/privatecircle/visit/controller/VisitRecordController.java`
- `src/main/java/com/mcm/privatecircle/visit/service/VisitRecordService.java`
- `PROJECT.md`

#### 테스트

- `src/test/java/com/mcm/privatecircle/ai/AiBriefCreateFlowTest.java`
- `src/test/java/com/mcm/privatecircle/ai/AiBriefPersistenceServiceTest.java`
- `src/test/java/com/mcm/privatecircle/ai/AiBriefSourceContractTest.java`
- `src/test/java/com/mcm/privatecircle/ai/AiBriefSourceReaderIntegrationTest.java`
- `src/test/java/com/mcm/privatecircle/ai/GoogleGeminiBriefClientTest.java`
- `src/test/java/com/mcm/privatecircle/mockdata/MockDataResourceContractTest.java`
- `src/test/java/com/mcm/privatecircle/mockdata/MockDataSeederIntegrationTest.java`
- `src/test/java/com/mcm/privatecircle/visit/VisitRecordControllerTest.java`
- `src/test/java/com/mcm/privatecircle/visit/VisitRecordServiceIntegrationTest.java`

### 1. 실제 로그인과 상품 이미지 제공

**기존 코드의 문제**

- 프론트가 백엔드 상품 목록을 사용해도 상품 응답에 `imageUrl`이 없어 실제 이미지 주소를 구성할 수 없었다.
- 웹 프론트에서 백엔드 API와 정적 이미지에 접근할 때 CORS 및 정적 이미지 접근 허용이 준비되지 않았다.
- 상품 시드 경로와 실제 정적 파일 구성이 맞지 않아 이미지가 표시되지 않았다.

**발생 원인**

- `ProductSummaryResponse`가 상품 이미지 컬럼을 응답 계약에 포함하지 않았다.
- Spring Security가 `/images/product/**`를 공개 경로로 허용하지 않았고 로컬 Expo 웹 origin에 대한 CORS 설정이 없었다.
- 프론트와 백엔드가 서로 다른 이미지 경로를 기준으로 사용했다.

**수정 방향 및 실제 수정 내용**

- 상품 응답 DTO와 변환 로직에 `imageUrl`을 추가했다.
- 로컬 웹 origin, API 메서드, `Authorization`/`Content-Type` 헤더를 허용하는 CORS 설정을 추가했다.
- `/images/product/**`를 인증 없이 조회 가능한 정적 리소스 경로로 열었다.
- 상품 시드의 이미지 경로를 정적 리소스 URL에 맞추고 실제 상품 이미지 19개를 교체했다.

**개선 결과**

- 프론트가 상품 API 응답만으로 백엔드 정적 이미지 URL을 구성할 수 있게 됐다.
- Expo 웹에서 로그인 API와 상품 이미지 요청이 Spring Security에 차단되지 않게 됐다.

### 2. 개발용 CA 계정과 지점별 스탬프 정합성

**기존 코드의 문제**

- 테스트할 CA 계정과 소속 매장이 충분하지 않아 여러 매장의 스탬프를 검증하기 어려웠다.
- 기존 DB에 매장이 일부 존재하면 시더가 나머지 매장을 추가하지 않아 설정된 CA의 소속 매장을 찾지 못할 수 있었다.
- 스탬프 발급 성공 여부와 중복 차단, 실제 커밋 여부를 콘솔에서 구분하기 어려웠다.

**발생 원인**

- 목 데이터 시더가 매장/상품만 처리하고 직원 계정과 CA 프로필은 생성하지 않았다.
- 매장 시딩이 전체 건수 기준으로 조기 종료되어 개별 누락 매장을 보완하지 못했다.
- 스탬프 서비스에 단계별 진단 로그가 없었다.

**수정 방향 및 실제 수정 내용**

- `application.yaml`에 `CA-1092`~`CA-1095` 개발 계정과 각각의 소속 매장을 설정했다.
- 설정 기반으로 `EmployeeAccount`와 `ClientAdvisor`를 생성 또는 갱신하고 비밀번호는 `PasswordEncoder`로 저장하도록 시더를 확장했다.
- 매장을 이름별로 중복 검사해 누락된 매장만 보충하도록 변경하고 필수 데모 매장을 리소스에 추가했다.
- 기존 CA의 이름과 소속 매장을 개발 설정에 맞게 갱신할 수 있도록 엔티티 메서드를 추가했다.
- 스탬프 요청, 대상 방문/고객/CA 확인, 중복 차단, DB flush, 트랜잭션 commit, 고객/CA 조회 결과에 진단 로그를 추가했다.

**개선 결과**

- 네 개 CA 계정으로 서로 다른 매장의 스탬프를 테스트할 수 있게 됐다.
- 로그인한 CA의 `storeId`와 방문 매장을 기준으로 발급·조회되는 과정을 로그에서 확인할 수 있게 됐다.
- 누락된 매장이 있는 기존 개발 DB에서도 시더가 필요한 매장을 보완한다.

### 3. Gemini AI 브리프 실제 호출과 최신 상담 기록 반영

**기존 코드의 문제**

- `requestRawJson()`이 외부 호출 비활성 예외만 던져 실제 AI 브리프를 생성할 수 없었다.
- API 키 누락, 인증 실패, 네트워크/시간 초과가 일반 AI 호출 실패로 섞여 원인 파악이 어려웠다.
- 기준 방문의 상담 기록이 AI 입력에서 빠져 최신 상담 내용과 `cautionNote`가 브리프에 반영되지 않을 수 있었다.
- 요청이 인증, 입력 수집, Gemini 호출, 저장 중 어느 단계에서 실패했는지 알기 어려웠다.

**발생 원인**

- Google Gen AI SDK 호출과 Structured Output 스키마가 구현되지 않은 상태였다.
- 소스 조회가 기준 방문보다 이전 기록만 조회하고 현재 방문 기록은 별도 필드로 전달하지 않았다.
- AI 예외 분류와 단계별 로그가 충분하지 않았다.

**수정 방향 및 실제 수정 내용**

- Google Gen AI Java SDK `Client`를 API 키 기반 일반 Gemini 모드로 생성하고 모델, timeout, JSON MIME type과 응답 스키마를 지정해 호출하도록 구현했다.
- 응답 필드를 `summary`, `visitPurposeSummary`, `interestSummary`, `cautionSummary`, `suggestedDirection`으로 고정하고 파싱·필수값 검증을 유지했다.
- 빈 API 키는 애플리케이션 시작을 막지 않고 실제 생성 요청 시 `AI_BRIEF_API_KEY_MISSING`으로 반환하도록 설정 검증과 예외를 분리했다.
- Gemini 401 인증 실패, 설정 누락, timeout/전송 실패, JSON 파싱 실패를 각각 분리해 실패 상태를 저장하도록 했다.
- `AiBriefSource`에 `currentVisitRecord`를 추가하고 기준 방문의 목적, 상담 내용, 스타일 변화, 주의사항을 AI 입력에 포함했다.
- 현재 방문의 `cautionNote`가 있으면 `cautionSummary`에 명시적으로 반영하도록 프롬프트 규칙을 강화했다.
- JWT 인증/인가, Controller 진입, 입력 조회, Gemini 요청/응답, 생성·실패 DB 저장에 진단 로그를 추가했다. API 키 값 자체는 로그에 남기지 않는다.
- `GOOGLE_API_KEY`, `GEMINI_MODEL`, `GEMINI_TIMEOUT` 환경 설정을 사용하도록 정리했다.

**개선 결과**

- 실제 Gemini 응답으로 CA용 AI 브리프를 생성하고 DB에 `GENERATED` 또는 `FAILED` 상태로 저장할 수 있게 됐다.
- 최신 상담 기록과 후속 응대 주의사항이 AI 입력 및 결과에 반영된다.
- 키 누락, 인증 실패, timeout, 파싱 실패를 API 오류 코드와 서버 로그로 구분할 수 있게 됐다.

### 4. 상담 기록 실제 삭제

**기존 코드의 문제**

- 프론트에는 상담 기록 삭제 버튼이 있었지만 백엔드에 삭제 API가 없어 DB 기록은 삭제할 수 없었다.
- 화면에서만 기록을 제거하면 AI 브리프 입력에는 기존 DB 기록이 계속 남는 불일치가 발생할 수 있었다.

**발생 원인**

- 방문 기록 API가 생성, 조회, 수정까지만 제공되고 삭제 흐름이 구현되지 않았다.

**수정 방향 및 실제 수정 내용**

- `DELETE /api/v1/visit-records/{visitRecordId}`를 추가하고 `PROJECT.md`의 API 계약에 반영했다.
- Service에서 CA 인증, 소속 매장 범위, 최초 작성 CA 일치 여부를 확인한 뒤 삭제하도록 구현했다.
- 작성자 삭제 성공과 다른 CA 삭제 차단을 Controller/통합 테스트에 추가했다.

**개선 결과**

- 허용된 CA가 삭제한 상담 기록이 화면뿐 아니라 DB에서도 제거된다.
- 다른 매장 또는 다른 작성자의 기록을 임의로 삭제할 수 없다.

### 5. 로컬 MySQL 실행 설정 정리

**기존 코드의 문제 및 원인**

- MySQL 검증 프로파일의 환경 변수명이 실제 로컬 환경의 `MYSQL_USERNAME`, `MYSQL_PASSWORD`와 달랐다.
- 개발 중 스키마 변경을 반영해야 하는 실행 환경에서 `validate`가 적용되면 애플리케이션이 시작되지 않을 수 있었다.

**수정 방향 및 실제 수정 내용**

- MySQL 사용자명과 비밀번호 환경 변수명을 로컬 설정과 맞췄다.
- 해당 검증 프로파일의 Hibernate DDL 설정을 `update`로 변경했다.

**개선 결과**

- 현재 로컬 환경 변수로 MySQL 프로파일을 실행하고 개발 스키마를 반영할 수 있게 됐다.

## FE

### 수정한 파일

#### 실행 및 연동 설정/문서

- `HackaThon/.claude/launch.json`
- `HackaThon/.env.example`
- `HackaThon/docs/backend-handoff/README.md`
- `HackaThon/docs/backend-handoff/frontend-api-spec.html`
- `HackaThon/docs/backend-handoff/frontend-handoff-tickets.html`
- `HackaThon/package-lock.json`

#### 앱 및 API

- `HackaThon/mobile/App.tsx`
- `HackaThon/mobile/api.ts`
- `HackaThon/src/api/contracts.ts`
- `HackaThon/src/mock/products.ts`
- `HackaThon/src/data/products.json`

### 1. 데모 로그인 제거와 실제 인증 연결

**기존 코드의 문제**

- 백엔드 URL이 없다고 판단되면 아이디와 비밀번호 검증 없이 데모 고객으로 즉시 진입했다.
- 로그아웃 후에도 메모리에 access token이 남을 수 있었다.
- 회원가입 화면은 이메일을 요구하는 것처럼 표시했지만 실제 백엔드는 `loginId`를 사용했다.

**발생 원인**

- `hasConnectedBackend()`에 따라 실제 인증을 건너뛰는 데모 분기가 남아 있었다.
- 화면 문구와 백엔드 회원가입 계약의 필드 의미가 달랐다.

**수정 방향 및 실제 수정 내용**

- 기본 API 주소를 `http://localhost:8080`으로 지정하고 로그인/회원가입이 항상 백엔드 인증 API를 호출하도록 변경했다.
- 빈 아이디/비밀번호 입력을 차단하고 고객 로그인 후 `/customers/me`의 실제 프로필을 앱 상태에 반영했다.
- 로그아웃 시 access token을 제거했다.
- 회원가입의 `이메일` 문구와 입력 힌트를 `아이디` 기준으로 변경했다.
- `.env.example`과 로컬 실행 설정 및 백엔드 연동 문서를 보완했다.

**개선 결과**

- 로그인 버튼만 눌러 김민준 데모 계정으로 진입하던 문제가 없어졌다.
- 실제 회원가입 계정과 CA 계정의 인증 결과를 기준으로 화면에 진입한다.

### 2. 백엔드 상품과 이미지 연결

**기존 코드의 문제**

- 홈/추천/저장 상품 화면이 프론트 목 상품만 사용해 백엔드 상품 변경이 반영되지 않았다.
- 상대 이미지 URL과 과거 시드 경로를 그대로 사용해 브라우저에서 이미지가 깨졌다.

**발생 원인**

- 상품 API 계약과 프론트 모델 변환 함수가 없었다.
- 이미지 경로에 API base URL을 결합하는 처리가 없었다.

**수정 방향 및 실제 수정 내용**

- `ProductSummaryResponse` 계약과 상품 목록 API를 추가했다.
- 로그인 후 추천 가능한 상품을 조회해 홈, 맞춤 추천, 저장 상품, CA 추천 화면에서 공통으로 사용하도록 변경했다.
- 백엔드 상대 경로를 절대 URL로 변환하고 과거 `/product-*.jpg` 경로를 상품 코드 기반 정적 URL로 보정했다.
- URL이 없거나 잘못된 경우 번들 이미지를 사용하는 fallback 매핑을 추가했다.
- 프론트 상품 데이터의 이미지 경로를 백엔드 정적 경로와 맞췄다.

**개선 결과**

- 백엔드 상품 목록과 실제 상품 이미지가 프론트 화면에 표시된다.
- 일부 이미지 URL이 비어 있거나 과거 형식이어도 번들 이미지로 대체된다.

### 3. CA 소속 매장과 지점별 스탬프 연결

**기존 코드의 문제**

- CA 이름과 현재 매장이 프론트에 하드코딩되어 로그인 계정의 실제 소속과 다를 수 있었다.
- 프론트에서 임의로 매장을 선택할 수 있어 JWT의 `storeId`와 화면의 매장명이 불일치했다.
- 동일한 방문 ID를 계속 재사용하거나 이미 스탬프가 발급된 방문을 다시 사용해 중복 발급 또는 미반영처럼 보이는 문제가 있었다.
- 고객 로그인 후 백엔드 스탬프를 다시 조회하지 않아 로그아웃/재로그인 시 화면에서 사라지는 것처럼 보였다.

**발생 원인**

- CA 로그인 후 직원 프로필을 조회하지 않았고 화면 상태가 데모 상수에 의존했다.
- 활성 방문 캐시가 발급 후 제거되지 않았으며 최신 방문의 기존 스탬프 여부를 확인하지 않았다.
- 고객 상태가 로컬 저장값만 사용됐다.

**수정 방향 및 실제 수정 내용**

- CA 로그인 후 `/employees/me`를 호출해 CA 이름과 소속 매장을 앱 상태 및 헤더에 반영했다.
- 매장 선택 UI를 제거하고 로그인 CA의 소속 매장을 발급 화면에 고정했다.
- 고객 검색 결과를 실제 API 데이터로 구성하고, 고객 로그인 시 내 스탬프 목록을 조회해 지점명·발급 CA·일시를 동기화했다.
- 발급 전 최신 방문이 이미 스탬프를 보유했는지 확인하고 필요하면 새 방문을 생성한다.
- 스탬프 요청에 `stampType: VISIT`를 명시하고 성공 후 활성 방문 캐시를 제거했다.
- 백엔드 오류 메시지를 화면에 표시하도록 공통 오류 추출 함수를 추가했다.

**개선 결과**

- CA별 소속 매장의 도장이 정확한 지점명으로 발급된다.
- 고객 재로그인 후에도 DB에 저장된 스탬프가 다시 표시된다.
- 같은 방문 ID 재사용으로 인한 중복 발급 혼동이 줄었다.

### 4. 상담 기록 생성·수정·삭제의 DB 연결

**기존 코드의 문제**

- 상담 기록 생성 후 프론트가 백엔드의 `visitRecordId`를 보관하지 않았다.
- 수정과 삭제는 로컬 배열만 변경해 DB와 화면이 달라졌고, 웹에서는 `Alert.alert` 다중 버튼 callback이 실행되지 않아 삭제 버튼이 동작하지 않았다.
- 이미 상담 기록이 있는 방문에 다시 저장해 `한 방문당 기록 한 개` 제약과 충돌할 수 있었다.
- `GET /visits/{visitId}/records` 응답을 페이지 구조로 잘못 해석했다.

**발생 원인**

- API 응답 ID 대신 `consultation-{timestamp}` 형태의 프론트 ID를 만들었다.
- 수정·삭제 API 호출이 상세 화면에 연결되지 않았다.
- 방문 목적별 선택 로직 없이 하나의 활성 방문을 상담, 스탬프, AI가 공유했다.

**수정 방향 및 실제 수정 내용**

- 상담 저장 응답의 실제 `visitRecordId`를 로컬 상담 기록 ID로 보관했다.
- 기록이 없는 방문을 선택하고, 없으면 새 방문을 생성하는 상담 전용 방문 선택 로직을 추가했다.
- 단건 상담 기록 응답 계약에 맞게 조회 함수를 수정했다.
- 수정은 `PATCH`, 삭제는 새 `DELETE` API가 성공한 뒤 로컬 상태를 변경하도록 연결했다.
- 과거 로컬 ID는 고객 방문 기록 중 생성 시각이 5분 이내로 가장 가까운 기록만 실제 ID로 복구한다.
- 웹에서는 브라우저 `confirm`, 모바일에서는 네이티브 `Alert`를 사용해 삭제 확인을 처리한다.

**개선 결과**

- 상담 기록의 생성·수정·삭제가 화면과 DB에 동일하게 반영된다.
- 삭제한 기록이 이후 AI 브리프 입력에 계속 남는 불일치를 방지한다.
- 동일 방문에 상담 기록을 반복 생성해 저장 버튼이 실패하던 문제를 줄였다.

### 5. AI 브리프 화면과 실제 API 흐름 연결

**기존 코드의 문제**

- AI 브리프 버튼이 목 상태 중심으로 동작하고 실제 고객/방문/상담 기록과 연결되는 과정이 불명확했다.
- 상담 기록이 있는 방문이 아니라 단순 최신 방문을 사용해 AI 입력이 비어 있을 수 있었다.
- 생성 중, 성공, 실패가 화면에서 명확히 구분되지 않았고 백엔드 오류 내용도 보이지 않았다.

**발생 원인**

- AI 생성용 방문 선택 로직과 API 요청 진단 로그가 없었다.
- 생성 API 응답 이후 최신 저장 결과를 다시 조회하지 않았다.
- 데모 고객 ID와 실제 숫자 customerId를 같은 흐름으로 처리했다.

**수정 방향 및 실제 수정 내용**

- 상담 기록이 존재하는 최신 방문을 AI 기준 방문으로 선택하도록 분리했다.
- `POST` 생성 후 `GET latest`로 저장된 최신 브리프를 다시 읽어 화면 모델로 변환했다.
- 실제 숫자 customerId를 가진 고객에게만 생성 버튼을 표시하고 데모 고객은 버튼을 숨겼다.
- `READY`, 생성 중, `LIVE AI`, 오류 메시지 상태를 화면에 표시하고 중복 클릭을 차단했다.
- AI 생성 요청 timeout을 일반 API보다 길게 설정하고 요청 URL, customerId, visitId, 인증 포함 여부, 응답/실패를 콘솔 로그로 확인할 수 있게 했다.
- 주의사항과 제안 필드를 실제 응답 필드에서 안전하게 변환하도록 정리했다.

**개선 결과**

- 실제 고객의 상담 기록을 기준으로 Gemini 브리프를 생성하고 최신 결과를 화면에 표시한다.
- 버튼이 반응하지 않는 것처럼 보이던 상황에서 진행 상태와 구체적인 실패 원인을 확인할 수 있다.
- 데모 고객과 실제 고객의 AI 생성 흐름이 분리됐다.

### 6. 기타 FE 변경

- `EmployeeProfileResponse`를 추가해 CA 프로필 응답 타입을 명시했다.
- 공통 `Button`에 disabled 상태와 시각적 비활성 표현을 추가했다.
- `npm install` 실행 결과에 따라 `package-lock.json`의 peer dependency 메타데이터가 갱신됐다. 애플리케이션 의존성 자체를 새로 추가한 변경은 아니다.

## 전체 요약

### BE 해결 사항

- 실제 Gemini SDK 호출, Structured JSON 응답, 최신 상담 기록/주의사항 반영과 실패 원인별 처리까지 AI 브리프 생성 흐름을 완성했다.
- 개발용 CA와 소속 매장을 구성하고 지점별 스탬프 발급·조회 정합성과 진단 로그를 보강했다.
- 상품 이미지 API/정적 리소스 제공과 상담 기록 권한 기반 삭제 API를 추가했다.

### FE 해결 사항

- 데모 자동 로그인 대신 실제 고객/CA 인증을 사용하고 백엔드 상품 이미지와 스탬프 데이터를 화면에 연결했다.
- CA 소속 매장 기준의 지점별 스탬프, 상담 기록의 실제 생성·수정·삭제, 실제 고객 AI 브리프 생성 흐름을 백엔드 API와 연결했다.
