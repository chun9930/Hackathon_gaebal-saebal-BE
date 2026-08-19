export type MembershipTier = 'VIP' | '일반 고객';

export interface JourneyStamp {
  id: string;
  storeName: string;
  type: 'visit' | 'purchase' | 'care' | 'invite';
  issuedAt: string;
  issuedByCA: string;
}

export interface PurchaseRecord {
  id: string;
  productName: string;
  variant: string;
  price: number;
  purchasedAt: string;
  storeName?: string;
  imageUrl?: string;
}

export interface CareRecord {
  id: string;
  type: string;
  note: string;
  date: string;
  storeName?: string;
}

export interface ConsultationNote {
  id: string;
  caName: string;
  storeName?: string;
  visitPurpose: string;
  content: string;
  styleChange?: string;
  cautionUpdate?: string;
  consentConfirmed: boolean;
  createdAt: string;
}

export interface Customer {
  id: string;
  name: string;
  customerNo: string;
  // API의 QR 조회는 customerNo가 아니라 이 토큰을 사용한다.
  qrToken?: string;
  phoneLast4: string; // Masked to last 4 digits per privacy principle
  membershipTier: MembershipTier;
  points: number;
  preferredStyle: string[];      // Qualitative
  purchasePurpose: string;       // Qualitative
  cautionNotes?: string;         // CA Only! Do not render on customer-facing views
  visitCount: number;
  joinedAt: string;
  avatarUrl?: string;
  stamps: JourneyStamp[];
  purchases: PurchaseRecord[];
  careRecords: CareRecord[];
  consultations: ConsultationNote[];
  savedProductIds: string[];
}

export interface AIBrief {
  customerId: string;
  summary: string;
  suggestedApproach: string;
  basis: string[];
  generatedAt: string;
  dataSource: string[];
  cautions: string[];
  mode: 'LIVE AI' | 'DEMO AI';
}

export interface ProductRecommendation {
  productId: string;
  productName: string;
  variant: string;
  price: number;
  tone: 'cognac' | 'black' | 'champagne';
  reason: string;
  category: string;
  // string은 API URL, number는 Expo에 번들된 전달 이미지(require)다.
  imageUrl: string | number;
  recommendable?: boolean;
}

export type UserRole = 'customer' | 'ca';
