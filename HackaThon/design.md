# MCM Private Circle Design Specification

> 이 문서는 현재 구현 및 배포된 MCM Private Circle의 디자인 기준을 코드에서 추출해 정리한 문서이다. 신규 화면과 컴포넌트는 이 문서를 따르며, 값이 충돌할 경우 실제 구현 파일을 최종 기준으로 삼는다.

## 1. 디자인 방향

MCM Private Circle은 럭셔리 매장의 사람 중심 응대를 지원하는 서비스이다. 화면은 기술을 전면에 드러내기보다 고객의 여정과 CA의 판단을 차분하게 정리하는 도구로 보여야 한다.

- **Human-first**: AI는 고객을 직접 응대하지 않고 CA의 기억과 판단을 지원한다.
- **Quiet luxury**: 과도한 장식, 그라데이션, 큰 곡률, 화려한 애니메이션을 사용하지 않는다.
- **Journey as heritage**: 방문, 상담, 구매, 케어 기록을 여권과 스탬프의 시각 언어로 표현한다.
- **Clear hierarchy**: 중요한 행동과 정보가 한눈에 구분되도록 대비와 여백을 사용한다.
- **Privacy by design**: 고객 정보는 필요한 범위만 노출하고 권한·동의·주의 상태를 명확히 표시한다.

## 2. 구현 환경

| 구분 | 기준 |
| --- | --- |
| Framework | React Native + Expo Router |
| Web | React Native Web |
| Styling | NativeWind / Tailwind CSS |
| Icon | Lucide React Native |
| Font | 기기 기본 System Sans |
| Radius | 기본 8px 이하 |
| 주요 화면 배경 | `#F7F7F5` |

## 3. 컬러 시스템

### 3.1 Core colors

| Token | Hex | 용도 |
| --- | --- | --- |
| `ink` | `#191714` | 주요 텍스트, 기본 버튼, 다크 패널 |
| `paper` | `#FFFDF9` | 카드와 입력 영역의 밝은 배경 |
| `cloud` | `#F4F1EB` | 아이콘 배경, 보조 영역 |
| `line` | `#D8D0C4` | 카드, 입력창, 구분선 |
| `gold` | `#B47A26` | 브랜드 강조, Eyebrow, 주요 아이콘 |
| `champagne` | `#E7C77D` | 다크 배경의 강조색, 여권 장식 |
| `wine` | `#7B2D35` | VIP, 주의, 오류, 스탬프 |
| `forest` | `#23493F` | 성공, 권한 확인, 개인정보 보호 |
| `muted` | `#736D65` | 설명, 메타데이터, 보조 텍스트 |

### 3.2 Supporting colors

| Hex | 용도 |
| --- | --- |
| `#F7F7F5` | 앱 전체 배경 |
| `#FAFAF8` | 입력창 배경 |
| `#ECEAE6` | 세그먼트 컨트롤 배경 |
| `#ECE8E1` | Neutral Pill과 중립 상태 |
| `#F4E6C4` | Gold Pill, 일정 및 고객 아바타 배경 |
| `#DCE8E2` | 성공, AI 근거, 보호 상태 배경 |
| `#F0DFE1` | VIP, 스탬프, 오류 상태 배경 |
| `#EFF5F2` | 개인정보·AI 원칙 안내 배경 |
| `#FCF5F5` | 주의·오류 안내 배경 |
| `#2D2A26` | Journey Passport 보조 다크 영역 |
| `#4A4640` | 다크 패널 내부 구분선 |

### 3.3 상태별 사용

- **기본 행동**: `ink` 배경 + 흰색 텍스트
- **보조 행동**: `paper` 배경 + `line` 테두리 + `ink` 텍스트
- **성공·확인·권한**: `forest`와 `#DCE8E2`
- **VIP·주의·오류**: `wine`과 `#F0DFE1` 또는 `#FCF5F5`
- **브랜드 강조**: `gold` 또는 다크 배경 위 `champagne`
- **AI 정보**: AI 자체를 별도 네온 컬러로 표현하지 않고 `forest`, `gold`, `ink` 안에서 처리한다.

## 4. 타이포그래피

별도 웹폰트를 사용하지 않으며 iOS, Android, Web의 System Sans를 사용한다. 자간은 기본값을 유지한다.

| 역할 | 크기 / 행간 | 굵기 | 색상 |
| --- | --- | --- | --- |
| Wordmark | 23px | Black | `ink` 또는 white |
| Wordmark subtitle | 9px | Semibold | `gold` 또는 `champagne` |
| Eyebrow | 11px | Bold, Uppercase | `gold` |
| Page title | 모바일 26/32px, 640px 이상 28/36px | Bold | `ink` |
| Result title | 24px | Bold | `ink` |
| Section title | 21px | Bold | `ink` |
| Card title | 16~18px | Bold | `ink` 또는 white |
| Body | 14~15px / 20~24px | Regular | `ink` 또는 `muted` |
| Button label | 15px | Semibold | 버튼 variant에 따름 |
| Caption / Meta | 10~12px / 16~20px | Regular~Semibold | `muted`, `gold` |

텍스트가 긴 경우 컨테이너에 `min-w-0`과 `flex-1`을 적용해 가로 레이아웃을 밀어내지 않게 한다. 버튼 라벨은 중앙 정렬하며 필요하면 여러 줄로 표시할 수 있다.

## 5. 레이아웃과 여백

### 5.1 Screen container

모든 일반 화면은 `AppScreen`을 사용한다.

| Width preset | 최대 폭 | 주요 용도 |
| --- | ---: | --- |
| `compact` | 560px | 성공, 오류, 빈 상태 |
| `content` | 820px | 여권, 여정, 폼, 브리프 |
| `wide` | 1180px | 로그인, CA 홈, 고객 상세, 추천 |

화면 좌우 여백은 다음과 같다.

- 0~639px: 16px
- 640~1023px: 24px
- 1024px 이상: 20px
- 화면 하단 기본 여백: 모바일 32px, 640px 이상 40px
- Safe Area의 top, left, right를 반영한다.
- 스크롤 화면은 모바일 키보드가 입력 영역을 가리지 않도록 자동 inset을 적용한다.

### 5.2 Spacing scale

기본 간격은 4px 배수를 사용한다.

| 값 | 용도 |
| ---: | --- |
| 4px | 텍스트 내부의 최소 간격 |
| 8px | 아이콘과 라벨, Pill 사이 |
| 12px | 버튼 그룹, 리스트 내부 |
| 16px | 카드 내부 요소, 기본 화면 간격 |
| 20px | 카드 기본 padding |
| 24px | 큰 카드, 폼, PageHeader 간격 |
| 28~36px | 섹션 간 수직 분리 |

### 5.3 Responsive breakpoints

| Breakpoint | 폭 | 적용 원칙 |
| --- | ---: | --- |
| Base | 0px 이상 | 스마트폰 세로, 한 열 중심 |
| `xs` | 360px 이상 | 고객 프로필을 가로로 배치하고 여권 미리보기 크기를 확장 |
| `sm` | 640px 이상 | 버튼 그룹을 가로로 배치하고 화면 여백을 24px로 확장 |
| `md` | 768px 이상 | 태블릿 2열 구조, 추천 카드 가로 배열 |
| `lg` | 1024px 이상 | 최대 폭 화면과 넓은 데스크톱 여백 적용 |

### 5.4 Device behavior

- **320~359px 스마트폰**: 고객 프로필은 정보와 Journey Passport Pill을 세로로 분리한다.
- **360px 이상 스마트폰**: 프로필과 상태를 한 행에서 보여준다.
- **스마트폰 공통**: 주요 카드와 행동은 한 열이며 터치 영역을 최소 40~48px로 유지한다.
- **768px 태블릿**: CA 홈의 QR/검색, 최근 고객/상담 현황, CA 고객 상세, 스탬프 확인을 2열로 전환한다.
- **태블릿 추천 화면**: 제품 후보 3개를 가로 카드로 배치한다.
- **1024×768 가로 태블릿**: `wide` 컨테이너 안에서 정보 밀도를 높이되 최대 폭을 넘기지 않는다.

## 6. 형태와 시각 규칙

- 기본 카드와 입력창의 radius는 **8px**이다.
- Pill, 아바타, 스탬프는 의미가 있을 때만 완전한 원형을 사용한다.
- 카드에는 얕은 테두리를 사용하고 기본 그림자는 사용하지 않는다.
- 페이지 섹션 전체를 떠 있는 카드처럼 만들지 않는다.
- 다크 패널은 `ink`를 사용하며 여권, AI 요약, 핵심 작업 진입점에 제한한다.
- 강조 구분은 색상 면보다 `2px` 좌측 선과 옅은 상태 배경을 우선 사용한다.
- 그라데이션, 장식용 오브, 강한 블러를 사용하지 않는다.

## 7. 공통 컴포넌트

### 7.1 AppButton

| Variant | 스타일 | 용도 |
| --- | --- | --- |
| `primary` | `ink` 배경, 흰색 텍스트 | 화면의 대표 행동 |
| `secondary` | `paper` 배경, `line` 테두리 | 취소 외 보조 행동 |
| `ghost` | 투명 배경과 테두리 | 낮은 우선순위 행동 |
| `danger` | `wine` 배경, 흰색 텍스트 | 위험 확인, 중복 발급 등 |

- 기본 높이: 최소 48px
- Compact 높이: 최소 40px
- 기본 좌우 padding: 20px
- 아이콘: 기본 18px, Compact 16px, stroke 1.8
- Pressed 또는 Disabled opacity: 0.62
- 한 영역의 primary button은 원칙적으로 하나만 둔다.

### 7.2 IconButton

- 크기: 44×44px
- 아이콘: 20px
- radius: 8px
- 일반 상태: 흰색 배경 + `line` 테두리
- 역상 상태: 반투명 검정 배경 + 흰색 테두리
- 뒤로가기, 로그아웃, 알림처럼 익숙한 단일 명령에 사용한다.

### 7.3 Surface

- 배경: `paper`
- 테두리: 1px `line`
- radius: 8px
- 기본 내부 여백: 20px
- 반복 항목, 정보 묶음, 폼 영역에 사용한다.

### 7.4 Pill

| Tone | 의미 |
| --- | --- |
| `gold` | 멤버십, 포인트, 선택 상태 |
| `forest` | 성공, 권한 확인, AI 데이터 출처 |
| `wine` | VIP, 관심 제품, 주의 상태 |
| `neutral` | 일반 등급, 보조 태그 |

Pill은 `11px Bold`, 좌우 10px, 상하 4px을 사용한다. 긴 문장을 Pill 안에 넣지 않는다.

### 7.5 SegmentedControl

- 외부 배경: `#ECEAE6`
- 외부 padding: 4px
- 최소 높이: 40px
- 활성 항목: 흰색 배경, `ink` 텍스트
- 비활성 항목: 투명 배경, `muted` 텍스트
- 스마트폰에서는 항목 폭을 동일하게 분배한다.

### 7.6 DataRow

- 상하 padding: 12px
- 아이콘 영역: 32×32px 원형 `cloud`
- 아이콘: 16px `gold`
- Label: 12px `muted`
- Value: 15/20px `ink`
- 연속된 DataRow 사이에는 1px Divider를 둔다.

### 7.7 ResultPanel

성공, 오류, 빈 상태에 사용한다.

- 상태 아이콘: 64×64px 원형
- 제목: 24px Bold, 중앙 정렬
- 설명: 최대 384px, 14/24px, 중앙 정렬
- 다음 행동은 세로 버튼 그룹으로 제공한다.

### 7.8 ProductCard

- 제품 시각 영역: 기본 높이 176px, Compact는 80×80px
- 제품별 tone은 cognac, black, champagne 세 가지로 구분한다.
- 카드 안에는 제품명, variant, 가격, 추천 근거, 저장 행동을 순서대로 배치한다.
- 고객용은 `FOR YOU`, CA용은 `CA PICK` Pill을 사용한다.
- 768px 이상에서 3개 카드가 같은 행을 공유한다.

### 7.9 JourneyStamp

- 정사각형 영역 안에 원형 스탬프를 배치한다.
- `wine` 테두리, `#FAF3ED` 배경을 사용한다.
- 반복 스탬프는 -2~2도의 작은 회전값을 달리해 실제 여권 도장의 인상을 준다.
- 매장명은 한 줄로 제한한다.

## 8. 내비게이션

### 8.1 고객용

하단 탭은 항상 고객의 현재 여정 안에서 이동하는 구조이다.

| 탭 | Icon | Route |
| --- | --- | --- |
| 홈 | House | `/customer` |
| 여권 | BookOpen | `/customer/passport` |
| 여정 | MapPinned | `/customer/journey` |
| 추천 | Sparkles | `/customer/recommendations` |
| 마이 | UserRound | `/customer/profile` |

- 탭 높이: 70px
- 상하 padding: 8px
- 아이콘: 20px
- Label: 10px Semibold
- 활성 색상: `ink`, 비활성 색상: `#8A847C`

### 8.2 CA용

CA 화면은 하단 탭을 사용하지 않는다. 업무 순서에 따라 `CA 홈 → 고객 식별 → 고객 상세 → 브리프/추천/상담 기록/스탬프`로 이동하며, 하위 화면에는 44px 뒤로가기 IconButton을 둔다.

## 9. 화면별 디자인 구조

### 9.1 공통 로그인

- 스마트폰: 브랜드 소개 패널과 로그인 폼을 세로 배치한다.
- 태블릿 이상: 브랜드 패널 42%, 로그인 폼 58%의 2열 구조로 전환한다.
- 브랜드 패널은 `ink`, 로그인 영역은 흰색을 사용한다.
- 고객/CA 역할은 SegmentedControl로 선택한다.

### 9.2 고객용 화면

| 화면 | Route | 핵심 구조 |
| --- | --- | --- |
| 고객 홈 | `/customer` | Wordmark, 고객 요약, Journey Passport, 최근 여정, 추천, 주요 행동 |
| Journey Passport | `/customer/passport` | 여권형 다크 카드, QR, 멤버십 진행도, 취향, 최근 여정 |
| 나의 여정 | `/customer/journey` | 방문 스탬프 타임라인 / 구매·케어 이력 세그먼트 |
| 추천 | `/customer/recommendations` | 취향 Pill, 응대 맥락, 제품 카드 3개 |
| 마이 | `/customer/profile` | 고객 정보, 취향, 알림·언어·동의·데이터 설정 |
| 혜택 | `/customer/benefits` | 현재 등급, 포인트 진행도, 사용 가능한 혜택 목록 |
| 결과 화면 | `/customer/saved`, `/customer/empty` | 관심 제품 저장, 빈 여정 등 단일 결과와 다음 행동 |

### 9.3 CA용 화면

| 화면 | Route | 핵심 구조 |
| --- | --- | --- |
| CA 홈 | `/ca` | QR 스캔, 고객 검색, 최근 응대 고객, 상담 현황 |
| QR 스캐너 | `/ca/scanner` | 전체 화면 카메라, 중앙 QR 프레임, 개인정보 안내 |
| 고객 검색 | `/ca/search` | 식별 방법, 검색 입력, 마스킹된 결과, 확인 행동 |
| 고객 상세 | `/ca/customer/[id]` | 고객 여권 요약, 정성·정량 정보, 여정 기록, 후속 메모 |
| AI 브리프 | `/ca/brief/[id]` | 상담 맥락 요약, 응대 제안, 근거, 주의사항, 재생성 |
| 제품 추천 | `/ca/recommendations/[id]` | 관심 태그, AI 추천 근거, CA용 제품 후보 |
| 상담 기록 | `/ca/consultation/[id]` | 방문 목적, 상담 내용, 취향 변화, 주의사항, 동의 확인 |
| 스탬프 발급 | `/ca/stamp/[id]` | 매장 도장, 고객·CA·시간 확인, 중복 확인, 발급 행동 |
| 결과 화면 | `/ca/unregistered`, `/ca/recommendation-saved` | 미등록 고객, 저장 완료, 발급 완료와 다음 업무 행동 |

## 10. AI 및 개인정보 표현 원칙

- AI 결과에는 **근거 기록**, **생성 시점**, **데이터 출처**, **주의사항**을 함께 표시한다.
- `LIVE AI`와 `DEMO AI` 상태를 Pill로 구분한다.
- AI의 제안은 명령형 확정 문구가 아니라 CA가 판단할 수 있는 참고 문구로 작성한다.
- 개인정보 안내는 `forest` 좌측선과 `#EFF5F2` 배경으로 표시한다.
- 검색 결과의 연락처는 전체 번호 대신 끝 4자리만 보여준다.
- 고객용 화면에는 내부 상담 메모 전체가 아니라 고객 공개용 요약만 제공한다.
- 위험하거나 중복 가능성이 있는 행동은 `wine` 상태와 재확인 단계를 사용한다.

## 11. 접근성 및 상호작용

- 모든 버튼은 `accessibilityRole="button"`과 의미 있는 label을 가진다.
- 아이콘 단독 버튼은 시각 텍스트가 없어도 접근성 label을 제공한다.
- 터치 영역은 Compact 40px, 일반 44~48px 이상으로 유지한다.
- 상태는 색상만으로 구분하지 않고 텍스트, 아이콘, Pill을 함께 사용한다.
- 입력창은 최소 48px 높이를 유지한다.
- Pressed 상태는 opacity 변화로 피드백을 제공한다.
- 스크롤바는 숨기되 화면 자체의 스크롤은 유지한다.
- 모바일 키보드 표시 시 입력 영역이 가려지지 않도록 자동 inset을 적용한다.

## 12. 디자인 QA 기준

신규 화면이나 레이아웃 수정 후 아래 해상도를 확인한다.

| 구분 | 기준 해상도 |
| --- | --- |
| 소형 스마트폰 | 320×700 |
| 일반 스마트폰 | 390×844 |
| 태블릿 세로 | 768×1024 |
| 태블릿 가로 | 1024×768 |
| 데스크톱 | 1440×900 |

확인 항목:

1. 가로 스크롤 또는 화면 밖으로 밀려난 요소가 없는가.
2. 긴 한국어·영문 텍스트가 버튼, Pill, 카드 경계를 침범하지 않는가.
3. 스마트폰에서 주요 행동이 한 열로 자연스럽게 이어지는가.
4. 태블릿에서 정보가 불필요하게 긴 한 열로 남지 않는가.
5. 입력창과 버튼의 높이 및 카드 내부 여백이 일관적인가.
6. 하단 탭과 본문 마지막 콘텐츠가 겹치지 않는가.
7. 성공, 주의, 오류, 권한 상태가 색상 외 정보로도 구분되는가.
8. AI 제안이 근거와 CA 최종 판단 원칙을 함께 표시하는가.

## 13. Source of truth

| 항목 | 구현 파일 |
| --- | --- |
| Color, breakpoint, radius | `tailwind.config.js` |
| 공통 화면·버튼·카드·타이포 | `src/components/mcm/ui.tsx` |
| 제품 카드 | `src/components/mcm/product.tsx` |
| AI 브리프 | `src/components/mcm/brief.tsx` |
| Journey Stamp | `src/components/mcm/stamp.tsx` |
| 고객 하단 탭 | `src/app/customer/_layout.tsx` |
| 고객 화면 | `src/app/customer/` |
| CA 화면 | `src/app/ca/` |

문서를 수정할 때는 위 구현 파일과 함께 변경하고, 반응형 QA 기준 해상도에서 다시 검증한다.
