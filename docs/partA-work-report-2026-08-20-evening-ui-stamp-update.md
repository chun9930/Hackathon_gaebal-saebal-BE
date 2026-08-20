# PartA Work Report - 2026-08-20 Evening UI / Stamp Update

## Scope

This document records the changes made on Thursday, August 20, 2026 after the earlier same-day work log, so they do not get confused with the dawn commit/history.

Covered work:

- Remove misleading CA home customer-search input UX
- Add backend-configured CORS support
- Remove CA-side dummy customer exposure and load DB customer data first
- Wire per-store journey stamp images from backend responses
- Update customer home section labels

## Changed Areas

### 1. CA customer search UX cleanup

File:

- `HackaThon/mobile/App.tsx`

Changes:

- Removed the non-functional text field shown in the CA home customer-search card
- Kept the action focused on navigation to the dedicated search screen
- Added explanatory copy so the button purpose is clear

Reason:

- The old input accepted typing but did not participate in actual search behavior, which caused confusion

### 2. CA customer list: dummy data removal

Files:

- `HackaThon/mobile/App.tsx`
- `src/main/java/com/mcm/privatecircle/customer/service/CustomerService.java`

Changes:

- On CA login, clear the customer list first and load database-backed customer data
- Added CA loading state handling in the mobile app
- Allowed blank-keyword customer search in backend service so CA can load the recent/entire customer list without fake seeded UI data
- Added empty-state handling when no customers are returned
- Refetched actual customer detail and stamp data on CA customer detail entry

Reason:

- CA screens were briefly showing frontend mock customers before the real API response arrived

### 3. Backend CORS configuration

Files:

- `src/main/java/com/mcm/privatecircle/global/config/CorsProperties.java`
- `src/main/java/com/mcm/privatecircle/global/security/SecurityConfig.java`
- `src/main/resources/application.yaml`

Changes:

- Added configuration-properties-based CORS setup
- Moved allowed origins, methods, headers, credentials, and max-age into application config
- Applied the configuration through Spring Security

Reason:

- This makes frontend connection policy explicit and easier to adjust per environment

### 4. Customer home section label updates

File:

- `HackaThon/mobile/App.tsx`

Changes:

- Changed the stamp section kicker from `PRIVATE CIRCLE` to `PASSPORT STAMP`
- Changed the recommendation section kicker from `PRIVATE CIRCLE` to `PRIVATE RECOMMEND`
- Extended the shared section-title component to accept a custom kicker

### 5. Backend stamp image mapping and delivery

Files:

- `src/main/java/com/mcm/privatecircle/stamp/dto/StampImageResolver.java`
- `src/main/java/com/mcm/privatecircle/stamp/dto/VisitStampResponse.java`
- `src/main/resources/static/images/stamps/*`
- `HackaThon/src/api/contracts.ts`
- `HackaThon/src/types/index.ts`
- `HackaThon/mobile/api.ts`
- `HackaThon/mobile/App.tsx`

Changes:

- Added store-name to stamp-image-path mapping for:
  - `MCM HAUS`
  - `MCM 롯데백화점 본점`
  - `MCM 롯데백화점 잠실점`
  - `MCM 신라면세점 서울점`
- Extracted the four stamp PNG files from the provided `stores.zip`
- Stored them under Spring static resources:
  - `src/main/resources/static/images/stamps/`
- Added `stampImageUrl` to the stamp response DTO
- Passed the stamp image URL through frontend contracts and journey-stamp state
- Made customer stamp rendering prefer backend image URLs and fall back to bundled local assets if image loading fails
- Applied the image URL both in:
  - customer home recent-stamp cards
  - `나의 여정` stamp timeline

### 6. Static stamp image access

File:

- `src/main/java/com/mcm/privatecircle/global/security/SecurityConfig.java`

Changes:

- Allowed unauthenticated access to `/images/stamps/**`

Reason:

- Without this, the frontend could receive a correct image URL but still fail to load the static image resource

## Verification Notes

Manual verification performed:

- Confirmed backend static stamp image URL returns HTTP `200`
- Confirmed `/api/v1/customers/me/stamps` response includes:
  - `storeName`
  - `stampImageUrl`
- Confirmed the extracted four stamp PNG files are distinct files, not duplicates

Observed result:

- The backend image mapping is working correctly
- The remaining visual similarity comes from the stamp design set itself being intentionally similar across Seoul stores, not from ERD or API mismatch

## Tests

Updated test files:

- `src/test/java/com/mcm/privatecircle/stamp/VisitStampControllerTest.java`
- `src/test/java/com/mcm/privatecircle/stamp/VisitStampServiceIntegrationTest.java`

Notes:

- Test source was updated to match the new `stampImageUrl` field
- Full automated test completion could not be conclusively confirmed from the local Gradle run output during this session

## Commit Scope Guidance

This report is intended to accompany the code changes listed above only.

It should remain separate from unrelated worktree changes such as:

- `HackaThon/.gitignore`
- `HackaThon/.env.example`
