import assert from 'node:assert/strict';
import test from 'node:test';

import { getCustomerSearchErrorMessage, normalizeCustomerSearchKeyword } from './customer-search.ts';

test('whitespace customer search keyword is rejected before API call', () => {
  assert.deepEqual(normalizeCustomerSearchKeyword('   \t  '), {
    ok: false,
    message: '검색어를 입력해 주세요.',
  });
});

test('valid customer search keyword is trimmed', () => {
  assert.deepEqual(normalizeCustomerSearchKeyword('  Alice  '), {
    ok: true,
    keyword: 'Alice',
  });
});

test('backend INVALID_REQUEST maps to the empty-search user message', () => {
  assert.equal(getCustomerSearchErrorMessage('INVALID_REQUEST'), '검색어를 입력해 주세요.');
});
