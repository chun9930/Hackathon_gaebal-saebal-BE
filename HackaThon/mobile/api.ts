import axios from 'axios';
import type { AIBrief, ConsultationNote, ProductRecommendation } from '../src/types';
import type { AIBriefResponse, ApiEnvelope, AuthTokens, ConsultationRecordRequest, ConsultationRecordResponse, CustomerProfileResponse, CustomerSearchItem, CustomerSignupRequest, PageEnvelope, StampResponse, VisitResponse } from '../src/api/contracts';

const FALLBACK_API_URL = 'https://api.example.com';
export const api = axios.create({ baseURL: process.env.EXPO_PUBLIC_API_URL ?? FALLBACK_API_URL, timeout: 10_000, headers: { 'Content-Type': 'application/json' } });
let accessToken: string | null = null;
export const setAccessToken = (token: string | null) => { accessToken = token; };
export const hasConnectedBackend = () => !api.defaults.baseURL?.includes('api.example.com');
api.interceptors.request.use((config) => { if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`; return config; });
const unwrap = <T>(response: { data: ApiEnvelope<T> }): T => { if (!response.data.success) throw new Error(response.data.message ?? '?遺욧퍕 筌ｌ꼶?????쎈솭??됰뮸??덈뼄.'); return response.data.data; };

export type ConsultationDraft = Pick<ConsultationNote, 'visitPurpose' | 'content' | 'styleChange' | 'cautionUpdate' | 'consentConfirmed'> & { storeName: string; caName: string };
export type AIInsightResponse = { brief: AIBrief; recommendations: ProductRecommendation[] };

const activeVisitIds = new Map<string, number>();

const nonEmpty = (value?: string | null): value is string => Boolean(value && value.trim());

const toBrief = (customerId: string, response: AIBriefResponse): AIBrief => {
  const basis = [response.visitPurposeSummary, response.interestSummary].filter(nonEmpty);
  const cautions = [response.cautionSummary].filter(nonEmpty);
  const summary = response.summary?.trim() || (response.status === 'FAILED'
    ? 'AI ?됰슢?????밴쉐????쎈솭??됰뮸??덈뼄. ?醫롫뻻 ????쇰뻻 ??뺣즲??雅뚯눘苑??'
    : '??밴쉐??AI ?됰슢??袁? ??곷뮸??덈뼄.');

  return {
    customerId,
    summary,
    suggestedApproach: response.suggestedDirection?.trim() || summary,
    basis,
    generatedAt: response.generatedAt,
    dataSource: ['Visit', 'VisitRecord', 'Interest', 'Purchase', 'AI Brief API'],
    cautions,
    mode: 'LIVE AI',
  };
};

const ensureActiveVisit = async (customerId: string) => {
  const inMemoryVisit = activeVisitIds.get(customerId);
  if (inMemoryVisit) return inMemoryVisit;

  const visits = await visitApi.listForCustomer(customerId);
  const latestVisit = visits.items[0];
  if (latestVisit?.visitId) {
    activeVisitIds.set(customerId, latestVisit.visitId);
    return latestVisit.visitId;
  }

  const createdVisit = await visitApi.create(customerId);
  activeVisitIds.set(customerId, createdVisit.visitId);
  return createdVisit.visitId;
};

export const authApi = {
  customerSignup: async (payload: CustomerSignupRequest) => {
    const tokens = await api.post<ApiEnvelope<AuthTokens>>('/api/v1/auth/customers/signup', payload).then(unwrap);
    setAccessToken(tokens.accessToken);
    return tokens;
  },
  customerLogin: async (loginId: string, password: string) => {
    const tokens = await api.post<ApiEnvelope<AuthTokens>>('/api/v1/auth/customers/login', { loginId, password }).then(unwrap);
    setAccessToken(tokens.accessToken);
    return tokens;
  },
  employeeLogin: async (loginId: string, password: string) => {
    const tokens = await api.post<ApiEnvelope<AuthTokens>>('/api/v1/auth/employees/login', { loginId, password }).then(unwrap);
    setAccessToken(tokens.accessToken);
    return tokens;
  },
};

export const customerApi = {
  me: () => api.get<ApiEnvelope<CustomerProfileResponse>>('/api/v1/customers/me').then(unwrap),
  updateMe: (payload: Partial<Pick<CustomerProfileResponse, 'name' | 'phoneNumber' | 'profileImageUrl'>>) => api.patch<ApiEnvelope<CustomerProfileResponse>>('/api/v1/customers/me', payload).then(unwrap),
  getById: (customerId: string) => api.get<ApiEnvelope<CustomerProfileResponse>>(`/api/v1/customers/${customerId}`).then(unwrap),
  getByQr: (qrToken: string) => api.get<ApiEnvelope<CustomerProfileResponse>>(`/api/v1/customers/by-qr/${qrToken}`).then(unwrap),
  search: (keyword: string, page = 0, size = 20) => api.get<PageEnvelope<CustomerSearchItem>>('/api/v1/customers/search', { params: { keyword, page, size } }).then(unwrap),
};

export const visitApi = {
  create: (customerId: string, visitedAt?: string) => api.post<ApiEnvelope<VisitResponse>>('/api/v1/visits', { customerId, visitedAt }).then(unwrap),
  get: (visitId: number) => api.get<ApiEnvelope<VisitResponse>>(`/api/v1/visits/${visitId}`).then(unwrap),
  listForCustomer: (customerId: string, page = 0, size = 20) => api.get<PageEnvelope<VisitResponse>>(`/api/v1/customers/${customerId}/visits`, { params: { page, size } }).then(unwrap),
  createRecord: (visitId: number, payload: ConsultationRecordRequest) => api.post<ApiEnvelope<ConsultationRecordResponse>>(`/api/v1/visits/${visitId}/records`, payload).then(unwrap),
  records: (visitId: number) => api.get<PageEnvelope<ConsultationRecordResponse>>(`/api/v1/visits/${visitId}/records`).then(unwrap),
  issueStamp: (visitId: number) => api.post<ApiEnvelope<StampResponse>>(`/api/v1/visits/${visitId}/stamps`).then(unwrap),
  customerStamps: () => api.get<PageEnvelope<StampResponse>>('/api/v1/customers/me/stamps').then(unwrap),
};

export const aiBriefApi = {
  generate: (customerId: string, visitId: number) => api.post<ApiEnvelope<AIBriefResponse>>(`/api/v1/customers/${customerId}/ai-briefs`, { visitId }).then(unwrap),
  latest: (customerId: string, visitId: number) => api.get<ApiEnvelope<AIBriefResponse>>(`/api/v1/customers/${customerId}/ai-briefs/latest`, { params: { visitId } }).then(unwrap),
  history: (customerId: string, page = 0, size = 20) => api.get<PageEnvelope<AIBriefResponse>>(`/api/v1/customers/${customerId}/ai-briefs`, { params: { page, size } }).then(unwrap),
};

export const caApi = {
  getTodayBrief: async (customerId: string) => {
    const visitId = await ensureActiveVisit(customerId);
    return toBrief(customerId, await aiBriefApi.latest(customerId, visitId));
  },
  createConsultation: async (customerId: string, draft: ConsultationDraft) => {
    const visitId = await ensureActiveVisit(customerId);
    return visitApi.createRecord(visitId, {
      visitPurpose: draft.visitPurpose,
      content: draft.content,
      styleChangeNote: draft.styleChange || undefined,
      cautionNote: draft.cautionUpdate || undefined,
      consentConfirmed: draft.consentConfirmed,
    });
  },
  regenerateCustomerInsights: async (customerId: string): Promise<AIInsightResponse> => {
    const visitId = await ensureActiveVisit(customerId);
    const response = await aiBriefApi.generate(customerId, visitId);
    return { brief: toBrief(customerId, response), recommendations: [] };
  },
  issueVisitStamp: async (customerId: string) => {
    const visitId = await ensureActiveVisit(customerId);
    return visitApi.issueStamp(visitId);
  },
};

