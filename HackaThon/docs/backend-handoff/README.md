# Backend handoff 적용 상태

## 이번 반영

- `src/data/products.json`: 전달받은 제품 목록을 고객 추천 화면의 기준 데이터로 사용합니다.
- `src/data/stores.json`: 전달받은 국내 매장 목록을 CA 매장 데이터의 기준으로 사용합니다.
- `assets/products/*-native.jpg`: Android에서 안정적으로 표시되는 JPEG 제품 이미지입니다. 기존 같은 이름의 캐시를 피하기 위해 새 파일명으로 번들링합니다.
- `mobile/api.ts`: 전달 문서의 고객, 방문, 방문기록, 스탬프, AI Brief API 계약용 함수가 포함되어 있습니다.

## 실제 백엔드 연결에 필요한 값

전달 문서에는 실행 가능한 API 서버 주소가 없습니다. 따라서 고객 로그인·프로필·검색 화면까지 실제 서버로 연결하려면 백엔드에서 아래 주소를 받아야 합니다.

1. 프로젝트 루트에서 `.env.example`을 `.env`로 복사합니다.
2. `EXPO_PUBLIC_API_URL`에 백엔드의 HTTPS 주소를 입력합니다.
3. Expo를 `npx expo start --clear`로 다시 시작합니다.

서버 주소를 받기 전에는 앱이 의도적으로 로컬 목업 데이터로 동작합니다. `https://api.example.com`은 실제 서버가 아닙니다.
