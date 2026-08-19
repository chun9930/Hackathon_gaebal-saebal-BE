import sourceStores from '../data/stores.json';

export type DomesticStore = { id: string; name: string; location: string };

// 전달받은 stores.json 기준. CA 선택 UI와 실제 API storeId 매핑에서 공통으로 사용한다.
export const DOMESTIC_STORES: DomesticStore[] = sourceStores.map(({ name, location }, index) => ({
  id: `store-${String(index + 1).padStart(2, '0')}`,
  name,
  location,
}));

export const findDomesticStore = (name: string) => DOMESTIC_STORES.find((store) => store.name === name);
