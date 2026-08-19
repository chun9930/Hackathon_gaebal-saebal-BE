import { ProductRecommendation } from '../types';
import sourceProducts from '../data/products.json';

// Backend products.json을 기준으로 한 로컬 폴백이다. API 연결 전에도 같은 상품/가격/사진을 보여 준다.
const PRODUCT_IMAGES = [
  require('../../assets/products/product-01-native.jpg'), require('../../assets/products/product-02-native.jpg'),
  require('../../assets/products/product-03-native.jpg'), require('../../assets/products/product-04-native.jpg'),
  require('../../assets/products/product-05-native.jpg'), require('../../assets/products/product-06-native.jpg'),
  require('../../assets/products/product-07-native.jpg'), require('../../assets/products/product-08-native.jpg'),
  require('../../assets/products/product-09-native.jpg'), require('../../assets/products/product-10-native.jpg'),
  require('../../assets/products/product-11-native.jpg'), require('../../assets/products/product-12-native.jpg'),
  require('../../assets/products/product-13-native.jpg'), require('../../assets/products/product-14-native.jpg'),
  require('../../assets/products/product-15-native.jpg'), require('../../assets/products/product-16-native.jpg'),
  require('../../assets/products/product-17-native.jpg'), require('../../assets/products/product-18-native.jpg'),
  require('../../assets/products/product-19-native.jpg'),
];

// 전달받은 products.json을 앱의 단일 기준 데이터로 쓴다.
// React Native는 동적 require를 지원하지 않으므로 이미지 파일만 위에서 정적으로 매핑한다.
const SOURCE_PRODUCTS = sourceProducts;

const TONES: ProductRecommendation['tone'][] = ['cognac', 'black', 'champagne'];

export const MOCK_PRODUCTS: ProductRecommendation[] = SOURCE_PRODUCTS.map(
  ({ productCode, name, category, price, recommendable }, index) => ({
    productId: productCode, productName: name, category, price, recommendable,
    variant: category === '가방' ? 'MCM Signature' : category,
    tone: TONES[index % TONES.length],
    reason: '고객님의 여정과 선호를 바탕으로 추천하는 MCM 제품입니다.',
    imageUrl: PRODUCT_IMAGES[index],
  }),
);

export const RECOMMENDABLE_PRODUCTS = MOCK_PRODUCTS.filter((product) => product.recommendable);
