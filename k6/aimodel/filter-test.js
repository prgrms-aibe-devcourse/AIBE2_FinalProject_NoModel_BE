import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';
import {
  generateSearchParams,
  getRandomKeyword,
  getRandomCategory,
  getRandomTag,
  performanceThresholds
} from '../utils/test-data.js';
import {
  checkSearchResponse,
  checkBasicResponse,
  validateSearchKeyword,
  validatePaginationParams
} from '../utils/common-checks.js';

// 필터링 전용 메트릭
const filterErrorRate = new Rate('filter_error_rate');
const filterResponseTime = new Trend('filter_response_time');
const priceFilterAccuracy = new Rate('price_filter_accuracy');
const complexFilterPerformance = new Trend('complex_filter_performance');

// 테스트 설정
export const options = {
  stages: [
    { duration: '30s', target: 3 },   // 워밍업
    { duration: '2m', target: 10 },   // 필터 테스트 부하
    { duration: '2m', target: 15 },   // 복잡한 필터 조합
    { duration: '1m', target: 5 },    // 쿨다운
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<400', 'p(99)<600'],
    http_req_failed: ['rate<0.02'],
    filter_error_rate: ['rate<0.02'],
    filter_response_time: ['p(95)<400'],
    price_filter_accuracy: ['rate>0.95'], // 95% 이상 정확도
  },
};

const BASE_URL = 'http://host.docker.internal:8080/api';

export default function() {
  const startTime = Date.now();

  // 다양한 필터링 시나리오 테스트
  testPriceFiltering();
  testCategoryFiltering();
  testTagFiltering();
  testComplexFiltering();
  testEdgeCaseFiltering();
  testFilterCombinations();

  const endTime = Date.now();
  filterResponseTime.add(endTime - startTime);

  sleep(Math.random() * 2 + 1);
}

// 가격 필터링 테스트 (무료/유료/전체)
function testPriceFiltering() {
  const testCases = [
    { isFree: true, description: '무료 모델만' },
    { isFree: false, description: '유료 모델만' },
    { isFree: null, description: '전체 모델' }
  ];

  testCases.forEach(testCase => {
    const params = generateSearchParams({
      keyword: getRandomKeyword(),
      isFree: testCase.isFree,
      page: 0,
      size: 20
    });

    const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);

    const success = checkSearchResponse(response, {
      maxDuration: performanceThresholds.filter.response_time_p95,
      validatePriceFilter: true,
      expectedFree: testCase.isFree
    });

    // 가격 필터 정확도 검증
    if (testCase.isFree !== null) {
      const accuracyCheck = check(response, {
        [`Price filter accuracy for ${testCase.description}`]: (r) => {
          try {
            const data = r.json();
            const models = data.data.content;

            if (models.length === 0) return true;

            const correctlyFiltered = models.every(model => {
              const modelIsFree = model.price === 0 || model.price === null || model.price === undefined;
              return testCase.isFree ? modelIsFree : !modelIsFree;
            });

            return correctlyFiltered;
          } catch (e) {
            return false;
          }
        }
      });

      priceFilterAccuracy.add(accuracyCheck ? 1 : 0);
    }

    if (!success) {
      filterErrorRate.add(1);
    }
  });
}

// 카테고리 필터링 테스트
function testCategoryFiltering() {
  const category = getRandomCategory();
  const params = generateSearchParams({
    category: category,
    page: 0,
    size: 15
  });

  const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);

  const success = check(response, {
    'Category filter status is 200': (r) => r.status === 200,
    'Category filter response time < 300ms': (r) => r.timings.duration < 300,
    'Category filter returns valid results': (r) => {
      try {
        const data = r.json();
        return data.success && Array.isArray(data.data.content);
      } catch (e) {
        return false;
      }
    },
    'Category filter accuracy': (r) => {
      try {
        const data = r.json();
        const models = data.data.content;

        if (models.length === 0) return true;

        // 카테고리가 정확히 필터링되었는지 확인
        return models.every(model =>
          model.category === category || !model.category // null/undefined 허용
        );
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    filterErrorRate.add(1);
  }
}

// 태그 필터링 테스트
function testTagFiltering() {
  const tag = getRandomTag();
  const params = generateSearchParams({
    tags: tag,
    page: 0,
    size: 15
  });

  const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);

  const success = checkSearchResponse(response, {
    maxDuration: 300
  });

  const tagAccuracy = check(response, {
    'Tag filter accuracy': (r) => {
      try {
        const data = r.json();
        const models = data.data.content;

        if (models.length === 0) return true;

        // 태그가 포함되어 있는지 확인 (배열 또는 문자열)
        return models.every(model => {
          if (!model.tags) return true; // 태그가 없으면 허용

          if (Array.isArray(model.tags)) {
            return model.tags.includes(tag);
          } else if (typeof model.tags === 'string') {
            return model.tags.includes(tag);
          }

          return true;
        });
      } catch (e) {
        return false;
      }
    }
  });

  if (!success || !tagAccuracy) {
    filterErrorRate.add(1);
  }
}

// 복잡한 필터 조합 테스트
function testComplexFiltering() {
  const startTime = Date.now();

  const params = generateSearchParams({
    keyword: getRandomKeyword(),
    category: getRandomCategory(),
    tags: getRandomTag(),
    isFree: Math.random() > 0.5 ? true : false,
    page: 0,
    size: 10
  });

  const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);

  const endTime = Date.now();
  complexFilterPerformance.add(endTime - startTime);

  const success = check(response, {
    'Complex filter status is 200': (r) => r.status === 200,
    'Complex filter response time < 400ms': (r) => r.timings.duration < 400,
    'Complex filter returns results': (r) => {
      try {
        const data = r.json();
        return data.success && data.data;
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    filterErrorRate.add(1);
  }
}

// 엣지 케이스 필터링 테스트
function testEdgeCaseFiltering() {
  const edgeCases = [
    // 빈 키워드
    { keyword: '', description: '빈 키워드' },
    // 특수 문자
    { keyword: '!@#$%', description: '특수 문자' },
    // 매우 긴 키워드
    { keyword: 'a'.repeat(100), description: '긴 키워드' },
    // 숫자만
    { keyword: '12345', description: '숫자 키워드' },
    // 한글
    { keyword: '인공지능', description: '한글 키워드' }
  ];

  edgeCases.forEach(testCase => {
    // 유효하지 않은 키워드는 스킵
    if (!validateSearchKeyword(testCase.keyword) && testCase.keyword !== '') {
      return;
    }

    const params = generateSearchParams({
      keyword: testCase.keyword,
      page: 0,
      size: 5
    });

    const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);

    const success = check(response, {
      [`Edge case ${testCase.description} handles gracefully`]: (r) =>
        r.status === 200 || r.status === 400, // 400도 허용 (잘못된 요청)
      [`Edge case ${testCase.description} response time < 500ms`]: (r) =>
        r.timings.duration < 500
    });

    if (!success) {
      filterErrorRate.add(1);
    }
  });
}

// 페이지네이션과 필터 조합 테스트
function testFilterCombinations() {
  const paginationTests = [
    { page: 0, size: 10 },
    { page: 1, size: 20 },
    { page: 2, size: 5 },
    { page: 5, size: 50 }
  ];

  paginationTests.forEach(pagination => {
    if (!validatePaginationParams(pagination.page, pagination.size)) {
      return; // 유효하지 않은 페이지네이션은 스킵
    }

    const params = generateSearchParams({
      keyword: getRandomKeyword(),
      isFree: Math.random() > 0.5,
      page: pagination.page,
      size: pagination.size
    });

    const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);

    const success = checkSearchResponse(response, {
      maxDuration: 350
    });

    const paginationCheck = check(response, {
      'Pagination with filter has correct page': (r) => {
        try {
          const data = r.json();
          return data.data.number === pagination.page;
        } catch (e) {
          return false;
        }
      },
      'Pagination with filter has correct size': (r) => {
        try {
          const data = r.json();
          const content = data.data.content;
          return content.length <= pagination.size;
        } catch (e) {
          return false;
        }
      }
    });

    if (!success || !paginationCheck) {
      filterErrorRate.add(1);
    }
  });
}

console.log('🔍 Starting AI Model Filter Performance Test');
console.log(`📊 Test configuration: ${JSON.stringify(options, null, 2)}`);