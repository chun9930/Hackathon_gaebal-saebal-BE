/** Backend handoff API contract (frontend-api-spec.html). */
export type ApiEnvelope<T> = { success: boolean; data: T; message?: string };
export type PageEnvelope<T> = ApiEnvelope<{ items: T[]; page: number; size: number; totalElements: number; totalPages: number; hasNext: boolean }>;
export type AuthTokens = {
  accessToken: string;
  tokenType: string;
  accountId: number;
  customerId?: number;
  caId?: number;
  storeId?: number;
  role: string;
};
export type CustomerProfileResponse = {
  customerId: number;
  customerNo: string;
  name: string;
  phoneNumber?: string;
  qrToken: string;
  profileImageUrl?: string;
  membershipGrade?: string;
  stylePreferences?: string;
  visitCount?: number;
  stampCount?: number;
  lastVisitedAt?: string;
  joinedAt?: string;
};
export type CustomerSignupRequest = { loginId: string; password: string; name: string; phoneNumber: string };
export type CustomerSearchItem = {
  customerId: number;
  customerNo: string;
  name: string;
  phoneNumber?: string;
  profileImageUrl?: string;
  membershipGrade?: string;
  joinedAt?: string;
};
export type VisitResponse = {
  visitId: number;
  customerId: number;
  customerName: string;
  storeId: number;
  storeName: string;
  visitedAt: string;
};
export type ConsultationRecordRequest = {
  visitPurpose: string;
  content: string;
  styleChangeNote?: string;
  cautionNote?: string;
  consentConfirmed: boolean;
};
export type ConsultationRecordResponse = ConsultationRecordRequest & {
  visitRecordId: number;
  visitId: number;
  customerName: string;
  caName: string;
  storeId: number;
  storeName: string;
  visitedAt: string;
  createdAt: string;
};
export type StampResponse = {
  stampId: number;
  visitId: number;
  customerId: number;
  customerName: string;
  storeId: number;
  storeName: string;
  issuedByCaId: number;
  issuedByCaName: string;
  stampType: string;
  issuedAt: string;
  visitedAt: string;
};
export type AIBriefResponse = {
  briefId: number;
  customerId: number;
  visitId: number;
  requestedByCaId?: number | null;
  summary?: string | null;
  visitPurposeSummary?: string | null;
  interestSummary?: string | null;
  cautionSummary?: string | null;
  suggestedDirection?: string | null;
  sourceVisitCount?: number | null;
  status: 'GENERATED' | 'FAILED';
  generatedAt: string;
};
