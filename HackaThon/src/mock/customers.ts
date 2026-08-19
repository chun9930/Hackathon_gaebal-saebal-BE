import { Customer } from '../types';

export const MOCK_CUSTOMERS: Customer[] = [
  {
    id: 'cust-01',
    name: '김민준',
    customerNo: 'MCM-8829-3910',
    phoneLast4: '3910',
    membershipTier: 'VIP',
    points: 48500,
    preferredStyle: ['코냑 비세토스', '비즈니스 출장', '미니멀 클래식', '모빌리티 백팩'],
    purchasePurpose: '글로벌 비즈니스 출장 및 이브닝 파티용 슬림 웨어러블백 구매',
    cautionNotes: '과도한 강매식 제안을 극도로 불호함. 강한 조명이나 혼잡한 시간대를 피하는 프라이빗 룸 응대 선호. 샴페인 음료 서비스 먼저 권유할 것.',
    visitCount: 14,
    joinedAt: '2023-04-12',
    avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
    stamps: [
      {
        id: 'stamp-01',
        storeName: 'MCM 하우스 플래그십스토어',
        type: 'visit',
        issuedAt: '2026-08-10T14:30:00Z',
        issuedByCA: 'CA 이현우'
      },
      {
        id: 'stamp-02',
        storeName: 'MCM 롯데백화점 잠실점',
        type: 'purchase',
        issuedAt: '2026-07-22T16:15:00Z',
        issuedByCA: 'CA 박서연'
      },
      {
        id: 'stamp-03',
        storeName: 'MCM 롯데백화점 본점',
        type: 'visit',
        issuedAt: '2026-05-18T11:00:00Z',
        issuedByCA: 'CA Sato Tanaka'
      },
      {
        id: 'stamp-04',
        storeName: 'MCM 신라면세점 서울점',
        type: 'care',
        issuedAt: '2026-03-05T15:40:00Z',
        issuedByCA: 'CA 최지은'
      }
    ],
    purchases: [
      {
        id: 'pur-01',
        productName: 'Aren 비세토스 메신저 백',
        variant: 'Cognac / Medium',
        price: 1090000,
        purchasedAt: '2026-07-22',
        storeName: 'MCM 롯데백화점 잠실점',
        imageUrl: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=400&q=80'
      },
      {
        id: 'pur-02',
        productName: 'Ottomar 비세토스 위켄더',
        variant: 'Cognac / Small',
        price: 1750000,
        purchasedAt: '2026-05-18',
        storeName: 'MCM 롯데백화점 본점',
        imageUrl: 'https://images.unsplash.com/photo-1581553680321-4fffae59febd?auto=format&fit=crop&w=400&q=80'
      }
    ],
    careRecords: [
      {
        id: 'care-01',
        type: '가죽 클리닝 & 모서리 보강',
        note: '클라라 토트 모서리 마모 보강 완료 (보증 기간 적용)',
        date: '2026-03-05',
        storeName: 'MCM 신라면세점 서울점'
      }
    ],
    consultations: [
      {
        id: 'con-01',
        caName: 'CA 이현우',
        visitPurpose: '가을 신상 캡슐 컬렉션 사전 프리뷰 및 가방 클리닝 상담',
        content: '유럽 출장 일정 공유 받음. 가볍고 수납력 있는 비세토스 라인업에 관심 표명.',
        styleChange: '기존 블랙 단색에서 코냑/샴페인 콤비 톤으로 취향 확장 중',
        cautionUpdate: '오후 3시 프리뷰 예약 시 조용한 단독 룸 배치 필수',
        consentConfirmed: true,
        createdAt: '2026-08-10'
      }
    ],
    savedProductIds: ['mcm-prod-01', 'mcm-prod-03']
  },
  {
    id: 'cust-02',
    name: 'Sarah Jenkins',
    customerNo: 'MCM-7192-1044',
    phoneLast4: '1044',
    membershipTier: '일반 고객',
    points: 29000,
    preferredStyle: ['스트리트 럭셔리', '네온 모티브', '파우치 / 오거나이저'],
    purchasePurpose: '디지털 노마드 워크용 컴팩트 오거나이저 구매',
    cautionNotes: '영문 응대 가능 조율 필요. 신제품 얼리 액세스 이벤트 시 메일 알림 요청함.',
    visitCount: 8,
    joinedAt: '2024-01-15',
    avatarUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=200&q=80',
    stamps: [
      {
        id: 'stamp-05',
        storeName: 'MCM 하우스 플래그십스토어',
        type: 'visit',
        issuedAt: '2026-08-01T13:20:00Z',
        issuedByCA: 'CA 이현우'
      },
      {
        id: 'stamp-06',
        storeName: 'MCM 제주 롯데면세점',
        type: 'purchase',
        issuedAt: '2026-04-10T17:00:00Z',
        issuedByCA: 'CA Alex Tan'
      }
    ],
    purchases: [
      {
        id: 'pur-03',
        productName: '뮌헨 모노그램 파우치 & 지갑 세트',
        variant: 'Black Leather',
        price: 620000,
        purchasedAt: '2026-04-10',
        storeName: 'MCM 제주 롯데면세점',
        imageUrl: 'https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=400&q=80'
      }
    ],
    careRecords: [],
    consultations: [
      {
        id: 'con-02',
        caName: 'CA 이현우',
        visitPurpose: '노트북 및 태블릿 수납용 슬림 파우치 비교',
        content: '맥북 14인치가 들어가는 오거나이저 크기 문의. 블랙 컬러 재고 시 연락 약속.',
        consentConfirmed: true,
        createdAt: '2026-08-01'
      }
    ],
    savedProductIds: ['mcm-prod-02']
  },
  {
    id: 'cust-03',
    name: '이지우',
    customerNo: 'MCM-5541-8812',
    phoneLast4: '8812',
    membershipTier: '일반 고객',
    points: 12000,
    preferredStyle: ['크로스바디', '미니백', '샴페인 골드'],
    purchasePurpose: '생일 프라이빗 파티 착용 포인트백 탐색',
    cautionNotes: '첫 VIP 방문 고객. 멤버십 혜택과 스탬프 수집 시스템 친절 안내 필요.',
    visitCount: 3,
    joinedAt: '2025-09-20',
    avatarUrl: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=200&q=80',
    stamps: [
      {
        id: 'stamp-07',
        storeName: 'MCM 현대면세점 무역센터점',
        type: 'visit',
        issuedAt: '2026-08-11T16:00:00Z',
        issuedByCA: 'CA 최지은'
      }
    ],
    purchases: [],
    careRecords: [],
    consultations: [],
    savedProductIds: ['mcm-prod-03']
  },
  {
    id: 'cust-04',
    name: 'Sato Kenji',
    customerNo: 'MCM-3390-4491',
    phoneLast4: '4491',
    membershipTier: 'VIP',
    points: 62000,
    preferredStyle: ['위켄더 백', '트래블 루기지', '코냑 모노그램'],
    purchasePurpose: '글로벌 비행용 기내 가방 및 여권 오거나이저 세트',
    cautionNotes: '일본어 안내 가능 CA 배치 우대. 결제 시 TAX FREE 절차 신속 안내.',
    visitCount: 19,
    joinedAt: '2022-11-03',
    avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80',
    stamps: [
      {
        id: 'stamp-08',
        storeName: 'MCM 하우스 플래그십스토어',
        type: 'purchase',
        issuedAt: '2026-06-30T15:20:00Z',
        issuedByCA: 'CA 박서연'
      }
    ],
    purchases: [
      {
        id: 'pur-04',
        productName: 'M Stark 비세토스 백팩',
        variant: 'Cognac / Medium',
        price: 1890000,
        purchasedAt: '2026-06-30',
        storeName: 'MCM 하우스 플래그십스토어',
        imageUrl: 'https://images.unsplash.com/photo-1565084888279-aca607ecce0c?auto=format&fit=crop&w=400&q=80'
      }
    ],
    careRecords: [],
    consultations: [],
    savedProductIds: ['mcm-prod-04']
  }
];
