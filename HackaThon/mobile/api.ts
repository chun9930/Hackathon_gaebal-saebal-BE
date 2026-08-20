import axios from 'axios';
import type { AIBrief, ConsultationNote, ProductRecommendation } from '../src/types';
import { getLocalProductImage } from '../src/mock/products';
import type { AIBriefResponse, ApiEnvelope, AuthTokens, ConsultationRecordRequest, ConsultationRecordResponse, CustomerProfileResponse, CustomerSearchItem, CustomerSignupRequest, EmployeeProfileResponse, PageEnvelope, ProductSummaryResponse, StampResponse, VisitResponse } from '../src/api/contracts';

const DEFAULT_API_URL = 'https://mcmprivatecircle.store';
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL?.trim() || DEFAULT_API_URL;
const AI_REQUEST_TIMEOUT_MS = 45_000;
export const api = axios.create({ baseURL: API_BASE_URL, timeout: 10_000, headers: { 'Content-Type': 'application/json' } });
let accessToken: string | null = null;
export const setAccessToken = (token: string | null) => { accessToken = token; };
export const resolveApiUrl = (path?: string | null): string | undefined => {
  if (!path) return undefined;
  try {
    return new URL(path, `${API_BASE_URL.replace(/\/+$/, '')}/`).toString();
  } catch {
    return undefined;
  }
};
api.interceptors.request.use((config) => { if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`; return config; });
const unwrap = <T>(response: { data: ApiEnvelope<T> }): T => { if (!response.data.success) throw new Error(response.data.message ?? '?遺욧퍕 筌ｌ꼶?????쎈솭??됰뮸??덈뼄.'); return response.data.data; };

type ApiErrorEnvelope = { message?: string; error?: { code?: string; message?: string } };

export const getApiErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError<ApiErrorEnvelope>(error)) {
    return error.response?.data?.error?.message
      ?? error.response?.data?.message
      ?? error.message;
  }
  return error instanceof Error ? error.message : '요청 처리 중 오류가 발생했습니다.';
};

export const getApiErrorCode = (error: unknown): string | undefined => {
  if (axios.isAxiosError<ApiErrorEnvelope>(error)) {
    return error.response?.data?.error?.code;
  }
  return undefined;
};

const currentLocalDateTime = (): string => {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
    + `T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
};

export type ConsultationDraft = Pick<ConsultationNote, 'visitPurpose' | 'content' | 'styleChange' | 'cautionUpdate' | 'consentConfirmed'> & { storeName: string; caName: string };
export type AIInsightResponse = { brief: AIBrief; recommendations: ProductRecommendation[] };

const activeVisitIds = new Map<string, number>();

const nonEmpty = (value?: string | null): value is string => Boolean(value && value.trim());

const resolveProductImage = (product: ProductSummaryResponse): string | number => {
  const legacySeedPath = product.imageUrl?.match(/^\/product-\d+\.jpg$/);
  const imagePath = legacySeedPath
    ? `/images/product/${product.productCode}.jpg`
    : product.imageUrl;

  const resolvedImageUrl = resolveApiUrl(imagePath);
  if (resolvedImageUrl) {
    return resolvedImageUrl;
  }

  return getLocalProductImage(product.productCode) ?? '';
};

const toProductRecommendation = (product: ProductSummaryResponse): ProductRecommendation => ({
  productId: String(product.productId),
  productName: product.name,
  category: product.category,
  price: product.price,
  recommendable: product.recommendable,
  variant: product.category === '가방' ? 'MCM Signature' : product.category,
  tone: 'cognac',
  reason: '고객님의 여정과 선호를 바탕으로 추천하는 MCM 제품입니다.',
  imageUrl: resolveProductImage(product),
});

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
  if (!/^\d+$/.test(customerId)) {
    throw new Error('실제 고객의 숫자 ID가 없어 AI 브리프를 생성할 수 없습니다.');
  }
  const inMemoryVisit = activeVisitIds.get(customerId);
  if (inMemoryVisit) {
    console.info('[AI BRIEF] active visit selected', { customerId, visitId: inMemoryVisit, source: 'memory' });
    return inMemoryVisit;
  }

  const visits = await visitApi.listForCustomer(customerId);
  const latestVisit = visits.items[0];
  if (latestVisit?.visitId) {
    const stamps = await customerApi.stampsById(customerId);
    const alreadyStamped = stamps.items.some((stamp) => stamp.visitId === latestVisit.visitId);
    if (!alreadyStamped) {
      activeVisitIds.set(customerId, latestVisit.visitId);
      console.info('[AI BRIEF] active visit selected', { customerId, visitId: latestVisit.visitId, source: 'latest-visit' });
      return latestVisit.visitId;
    }
  }

  const createdVisit = await visitApi.create(customerId);
  activeVisitIds.set(customerId, createdVisit.visitId);
  console.info('[AI BRIEF] active visit selected', { customerId, visitId: createdVisit.visitId, source: 'created-visit' });
  return createdVisit.visitId;
};

const ensureBriefVisit = async (customerId: string) => {
  const inMemoryVisit = activeVisitIds.get(customerId);
  if (inMemoryVisit) {
    const inMemoryRecord = await visitApi.record(inMemoryVisit).catch(() => null);
    if (inMemoryRecord) {
      console.info('[AI BRIEF] brief visit selected', { customerId, visitId: inMemoryVisit, source: 'memory-with-record' });
      return inMemoryVisit;
    }
  }

  const visits = await visitApi.listForCustomer(customerId);
  for (const visit of visits.items) {
    const record = await visitApi.record(visit.visitId).catch(() => null);
    if (record) {
      activeVisitIds.set(customerId, visit.visitId);
      console.info('[AI BRIEF] brief visit selected', { customerId, visitId: visit.visitId, source: 'latest-visit-with-record' });
      return visit.visitId;
    }
  }

  return ensureActiveVisit(customerId);
};

const ensureConsultationVisit = async (customerId: string) => {
  if (!/^\d+$/.test(customerId)) {
    throw new Error('실제 고객의 숫자 ID가 없어 상담 기록을 저장할 수 없습니다.');
  }

  const inMemoryVisit = activeVisitIds.get(customerId);
  if (inMemoryVisit) {
    const inMemoryRecord = await visitApi.record(inMemoryVisit).catch(() => null);
    if (!inMemoryRecord) {
      console.info('[CONSULTATION] active visit selected', { customerId, visitId: inMemoryVisit, source: 'memory' });
      return inMemoryVisit;
    }
  }

  const visits = await visitApi.listForCustomer(customerId);
  for (const visit of visits.items) {
    const record = await visitApi.record(visit.visitId).catch(() => null);
    if (!record) {
      activeVisitIds.set(customerId, visit.visitId);
      console.info('[CONSULTATION] active visit selected', { customerId, visitId: visit.visitId, source: 'latest-visit-without-record' });
      return visit.visitId;
    }
  }

  const createdVisit = await visitApi.create(customerId);
  activeVisitIds.set(customerId, createdVisit.visitId);
  console.info('[CONSULTATION] active visit selected', { customerId, visitId: createdVisit.visitId, source: 'created-visit' });
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

export const employeeApi = {
  me: () => api.get<ApiEnvelope<EmployeeProfileResponse>>('/api/v1/employees/me').then(unwrap),
};

export const customerApi = {
  me: () => api.get<ApiEnvelope<CustomerProfileResponse>>('/api/v1/customers/me').then(unwrap),
  updateMe: (payload: Partial<Pick<CustomerProfileResponse, 'name' | 'phoneNumber' | 'profileImageUrl'>>) => api.patch<ApiEnvelope<CustomerProfileResponse>>('/api/v1/customers/me', payload).then(unwrap),
  getById: (customerId: string) => api.get<ApiEnvelope<CustomerProfileResponse>>(`/api/v1/customers/${customerId}`).then(unwrap),
  getByQr: (qrToken: string) => api.get<ApiEnvelope<CustomerProfileResponse>>(`/api/v1/customers/by-qr/${qrToken}`).then(unwrap),
  search: (keyword: string, page = 0, size = 20) => api.get<PageEnvelope<CustomerSearchItem>>('/api/v1/customers/search', { params: { keyword, page, size } }).then(unwrap),
  stamps: () => api.get<PageEnvelope<StampResponse>>('/api/v1/customers/me/stamps').then(unwrap),
  stampsById: (customerId: string, page = 0, size = 20) => api.get<PageEnvelope<StampResponse>>(`/api/v1/customers/${customerId}/stamps`, { params: { page, size } }).then(unwrap),
};

export const productApi = {
  list: () => api.get<ApiEnvelope<ProductSummaryResponse[]>>('/api/v1/products')
    .then(unwrap)
    .then((products) => products.map(toProductRecommendation)),
};

export const visitApi = {
  create: (customerId: string, visitedAt = currentLocalDateTime()) => api.post<ApiEnvelope<VisitResponse>>('/api/v1/visits', { customerId, visitedAt }).then(unwrap),
  get: (visitId: number) => api.get<ApiEnvelope<VisitResponse>>(`/api/v1/visits/${visitId}`).then(unwrap),
  listForCustomer: (customerId: string, page = 0, size = 20) => api.get<PageEnvelope<VisitResponse>>(`/api/v1/customers/${customerId}/visits`, { params: { page, size } }).then(unwrap),
  createRecord: (visitId: number, payload: ConsultationRecordRequest) => api.post<ApiEnvelope<ConsultationRecordResponse>>(`/api/v1/visits/${visitId}/records`, payload).then(unwrap),
  record: (visitId: number) => api.get<ApiEnvelope<ConsultationRecordResponse>>(`/api/v1/visits/${visitId}/records`).then(unwrap),
  updateRecord: (visitRecordId: number, payload: Omit<ConsultationRecordRequest, 'consentConfirmed'>) => api.patch<ApiEnvelope<ConsultationRecordResponse>>(`/api/v1/visit-records/${visitRecordId}`, payload).then(unwrap),
  deleteRecord: (visitRecordId: number) => api.delete<ApiEnvelope<null>>(`/api/v1/visit-records/${visitRecordId}`).then(unwrap),
  issueStamp: (visitId: number) => api.post<ApiEnvelope<StampResponse>>(
    `/api/v1/visits/${visitId}/stamps`,
    { stampType: 'VISIT' },
  ).then(unwrap),
  customerStamps: () => api.get<PageEnvelope<StampResponse>>('/api/v1/customers/me/stamps').then(unwrap),
};

const resolveVisitRecordId = async (customerId: string, recordId: string, createdAt: string) => {
  const numericId = Number(recordId);
  if (Number.isSafeInteger(numericId) && numericId > 0) return numericId;

  const visits = await visitApi.listForCustomer(customerId, 0, 100);
  const records = (await Promise.all(
    visits.items.map((visit) => visitApi.record(visit.visitId).catch(() => null)),
  )).filter((record): record is ConsultationRecordResponse => record !== null);
  const targetTime = Date.parse(createdAt);
  const closest = records.sort((left, right) =>
    Math.abs(Date.parse(left.createdAt) - targetTime) - Math.abs(Date.parse(right.createdAt) - targetTime),
  )[0];
  const isCloseEnough = closest
    && Number.isFinite(targetTime)
    && Math.abs(Date.parse(closest.createdAt) - targetTime) <= 5 * 60 * 1000;
  if (!isCloseEnough) {
    throw new Error('백엔드에서 연결된 상담 기록을 찾을 수 없습니다.');
  }
  return closest.visitRecordId;
};

export const aiBriefApi = {
  generate: async (customerId: string, visitId: number) => {
    const url = `/api/v1/customers/${customerId}/ai-briefs`;
    console.info('[AI BRIEF] POST request', {
      url,
      customerId,
      visitId,
      body: { visitId },
      authorizationIncluded: Boolean(accessToken),
    });
    try {
      const response = await api.post<ApiEnvelope<AIBriefResponse>>(url, { visitId }, { timeout: AI_REQUEST_TIMEOUT_MS });
      console.info('[AI BRIEF] POST response', { url, status: response.status, body: response.data });
      return unwrap(response);
    } catch (error) {
      if (axios.isAxiosError(error)) {
        console.error('[AI BRIEF] POST failed', { url, status: error.response?.status, body: error.response?.data, message: error.message });
      }
      throw error;
    }
  },
  latest: async (customerId: string, visitId: number) => {
    const url = `/api/v1/customers/${customerId}/ai-briefs/latest`;
    console.info('[AI BRIEF] GET latest request', {
      url,
      customerId,
      visitId,
      params: { visitId },
      authorizationIncluded: Boolean(accessToken),
    });
    try {
      const response = await api.get<ApiEnvelope<AIBriefResponse>>(url, { params: { visitId } });
      console.info('[AI BRIEF] GET latest response', { url, status: response.status, body: response.data });
      return unwrap(response);
    } catch (error) {
      if (axios.isAxiosError(error)) {
        console.error('[AI BRIEF] GET latest failed', { url, status: error.response?.status, body: error.response?.data, message: error.message });
      }
      throw error;
    }
  },
  history: (customerId: string, page = 0, size = 20) => api.get<PageEnvelope<AIBriefResponse>>(`/api/v1/customers/${customerId}/ai-briefs`, { params: { page, size } }).then(unwrap),
};

export const caApi = {
  getTodayBrief: async (customerId: string) => {
    const visitId = await ensureBriefVisit(customerId);
    return toBrief(customerId, await aiBriefApi.latest(customerId, visitId));
  },
  createConsultation: async (customerId: string, draft: ConsultationDraft) => {
    const visitId = await ensureConsultationVisit(customerId);
    return visitApi.createRecord(visitId, {
      visitPurpose: draft.visitPurpose,
      content: draft.content,
      styleChangeNote: draft.styleChange || undefined,
      cautionNote: draft.cautionUpdate || undefined,
      consentConfirmed: draft.consentConfirmed,
    });
  },
  updateConsultation: async (customerId: string, recordId: string, createdAt: string, draft: ConsultationDraft) => {
    const visitRecordId = await resolveVisitRecordId(customerId, recordId, createdAt);
    return visitApi.updateRecord(visitRecordId, {
      visitPurpose: draft.visitPurpose,
      content: draft.content,
      styleChangeNote: draft.styleChange || undefined,
      cautionNote: draft.cautionUpdate || undefined,
    });
  },
  deleteConsultation: async (customerId: string, recordId: string, createdAt: string) => {
    const visitRecordId = await resolveVisitRecordId(customerId, recordId, createdAt);
    return visitApi.deleteRecord(visitRecordId);
  },
  regenerateCustomerInsights: async (customerId: string): Promise<AIInsightResponse> => {
    const visitId = await ensureBriefVisit(customerId);
    await aiBriefApi.generate(customerId, visitId);
    const latest = await aiBriefApi.latest(customerId, visitId);
    const brief = toBrief(customerId, latest);
    console.info('[AI BRIEF] screen state payload', { customerId, visitId, brief });
    return { brief, recommendations: [] };
  },
  issueVisitStamp: async (customerId: string) => {
    const visitId = await ensureActiveVisit(customerId);
    const stamp = await visitApi.issueStamp(visitId);
    activeVisitIds.delete(customerId);
    return stamp;
  },
};

