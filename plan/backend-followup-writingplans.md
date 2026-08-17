# 백엔드 후속 보완 구현 계획

작성일: 2026.08.17

## 범위

이번 계획에서 요청된 항목은 구현 완료했다.

1. `customerNo` 정책 확정 및 `/customers/me` 노출
2. 고객 self-service 수정에서 `membershipGrade` 제한
3. CA 고객 검색 API 추가
4. Visit / VisitRecord / Stamp 조회 응답 display 필드 확장

추가 후속 항목은 AI 브리프 생성 API의 실제 외부 Gemini 호출 활성화 시점 관리다.

## 구현 결과

### 1. customerNo 정책 확정

- 회원가입 직후 저장된 고객 PK를 사용한다.
- 포맷은 `C%08d`로 고정한다.
- 별도 시퀀스, 날짜 조합, UUID 축약 정책은 사용하지 않는다.
- 기존 `qrToken` 생성 정책은 유지한다.

### 2. CustomerProfileResponse 확장

적용 API:

- `GET /api/v1/customers/me`
- `GET /api/v1/customers/{customerId}`
- `GET /api/v1/customers/by-qr/{qrToken}`

반영 필드:

- `customerNo`
- `qrToken`

### 3. Customer self-service 수정 제한

적용 API:

- `PATCH /api/v1/customers/me`

반영 내용:

- 요청 DTO에서 `membershipGrade` 제거
- Service update 로직에서도 `membershipGrade` 변경 제거
- 응답의 `membershipGrade`는 유지

### 4. CA 고객 검색 API 추가

적용 API:

- `GET /api/v1/customers/search?keyword=...&page=0&size=20`

반영 내용:

- `name`, `phoneNumber`, `customerNo` 기준 통합 검색
- `PageResponse<CustomerSearchResponse>` 반환
- 빈 검색어는 `INVALID_REQUEST(400)`
- 정렬은 `joinedAt DESC, id DESC`
- 목록 응답에는 `qrToken` 미포함

### 5. Visit / VisitRecord / Stamp display 필드 확장

반영 DTO:

- `VisitResponse`
- `VisitRecordResponse`
- `VisitStampResponse`

반영 내용:

- VisitResponse: `customerName`, `storeName`
- VisitRecordResponse: `customerName`, `caName`, `storeId`, `storeName`, `visitedAt`
- VisitStampResponse: `customerName`, `storeId`, `storeName`, `issuedByCaName`, `visitedAt`

## 검증 결과

- `*AuthServiceIntegrationTest` 성공
- `*ProfileServiceIntegrationTest` 성공
- `*CustomerSearchIntegrationTest` 성공
- `*CustomerControllerTest` 성공
- `*VisitControllerTest` 성공
- `*VisitRecordControllerTest` 성공
- `*VisitStampControllerTest` 성공
- `*VisitServiceIntegrationTest` 성공
- `*VisitRecordServiceIntegrationTest` 성공
- `*VisitStampServiceIntegrationTest` 성공
- 전체 `./gradlew.bat test` 성공

## 문서 변경 여부

결론: 프론트 전달용 API 명세서는 바뀐다.

사유:

- 고객 프로필 조회 응답 필드가 추가됐다.
- 고객 프로필 수정 요청 필드가 제거됐다.
- CA 고객 검색 endpoint와 응답 구조가 추가됐다.
- Visit / VisitRecord / Stamp 조회 응답에 display 필드가 추가됐다.