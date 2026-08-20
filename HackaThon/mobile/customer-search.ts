export type CustomerSearchKeywordResult =
  | { ok: true; keyword: string }
  | { ok: false; message: string };

export const EMPTY_CUSTOMER_SEARCH_MESSAGE = '검색어를 입력해 주세요.';

export const normalizeCustomerSearchKeyword = (value: string): CustomerSearchKeywordResult => {
  const keyword = value.trim();
  return keyword ? { ok: true, keyword } : { ok: false, message: EMPTY_CUSTOMER_SEARCH_MESSAGE };
};

export const getCustomerSearchErrorMessage = (code?: string | null): string | undefined => {
  if (code === 'INVALID_REQUEST') return EMPTY_CUSTOMER_SEARCH_MESSAGE;
  return undefined;
};
