# MCM Korea Journey Passport Store Stamps

MCM 공식 스토어 로케이터에서 2026-08-13 기준 대한민국 좌표에 등록된 14개 지점을 대상으로 제작한 Journey Passport 도장 자산이다.

## 산출물

- `journey-stamp-<store>.svg`: 편집 가능한 벡터 원본
- `journey-stamp-<store>.png`: 1024 x 1024 투명 PNG
- `journey-stamp-<store>-96.png`: 앱 스탬프 목록용 96 x 96 투명 PNG
- `mcm-korea-store-stamps-contact-sheet.jpg`: 전체 지점 미리보기
- `mcm-korea-stores.json`: 공식 로케이터 기반 지점 데이터와 도장 코드

도장 이미지에는 날짜나 방문 횟수를 고정하지 않았다. 실제 발급 시 앱에서 날짜, 방문 순번, CA 식별값을 별도 데이터로 기록해야 한 도장을 여러 방문에 재사용할 수 있다.

## 매장 범위

서울 8개, 부산 1개, 대구 1개, 인천 1개, 경기 1개, 제주 2개로 총 14개이다. 검색엔진에 남아 있더라도 현재 공식 스토어 로케이터 원본 데이터에 포함되지 않은 지점은 제외했다.

## 재생성

```powershell
py -3 scripts/generate-store-stamps.py
```

지점 목록과 운영 여부는 바뀔 수 있으므로 실제 서비스 배포 전 공식 로케이터를 다시 확인해야 한다.

- 공식 출처: <https://kr.mcmworldwide.com/ko_KR/storelocator>
