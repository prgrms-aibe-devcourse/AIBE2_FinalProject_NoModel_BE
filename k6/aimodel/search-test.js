import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// 커스텀 메트릭 정의
const searchErrorRate = new Rate('search_error_rate');
const searchResponseTime = new Trend('search_response_time');
const activeSearchUsers = new Gauge('active_search_users');
const searchRequestCount = new Counter('search_request_count');

// 환경 변수에 따른 테스트 설정
const TEST_TYPE = __ENV.TEST_TYPE || 'load';

// 스모크 테스트 설정 (최소한의 부하)
const smokeTestOptions = {
  stages: [
    { duration: '30s', target: 1 },   // 30초 동안 1명
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    search_error_rate: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)'],
};

// AI 모델 검색 테스트 설정
const searchTestOptions = {
  stages: [
    { duration: '30s', target: 5 },   // 워밍업: 30초 동안 5명까지
    { duration: '1m', target: 15 },   // 부하 증가: 1분 동안 15명까지
    { duration: '3m', target: 30 },   // 피크 부하: 3분 동안 30명 유지
    { duration: '1m', target: 15 },   // 부하 감소: 1분 동안 15명까지 감소
    { duration: '30s', target: 0 },   // 쿨다운: 30초 동안 0명까지
  ],
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<500'], // 검색 API 응답 시간
    http_req_failed: ['rate<0.02'],                 // 실패율 2% 미만
    search_error_rate: ['rate<0.02'],               // 검색 에러율 2% 미만
    search_response_time: ['p(95)<300'],            // 검색 응답시간 95% 300ms 미만
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
};

// 스트레스 테스트 설정
const stressTestOptions = {
  stages: [
    { duration: '2m', target: 50 },    // 2분 동안 50명까지
    { duration: '5m', target: 100 },   // 5분 동안 100명까지
    { duration: '3m', target: 150 },   // 3분 동안 150명까지
    { duration: '5m', target: 100 },   // 5분 동안 100명으로 감소
    { duration: '2m', target: 0 },     // 2분 동안 0명까지
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.05'],
    search_error_rate: ['rate<0.05'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
};

// 스파이크 테스트 설정
const spikeTestOptions = {
  stages: [
    { duration: '30s', target: 10 },   // 워밍업
    { duration: '30s', target: 200 },  // 급격한 부하 증가
    { duration: '1m', target: 200 },   // 높은 부하 유지
    { duration: '30s', target: 10 },   // 급격한 부하 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.10'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
};

// 테스트 설정 선택
let selectedOptions;
switch (TEST_TYPE) {
  case 'smoke':
    selectedOptions = smokeTestOptions;
    break;
  case 'stress':
    selectedOptions = stressTestOptions;
    break;
  case 'spike':
    selectedOptions = spikeTestOptions;
    break;
  default: // 'load'
    selectedOptions = searchTestOptions;
}

export const options = selectedOptions;

// 기본 URL 설정
const BASE_URL = 'http://host.docker.internal:8080/api';

// 테스트 데이터
const testSearchData = {
  keywords: [
    'AI', 'machine learning', 'deep learning', 'neural network',
    'computer vision', 'natural language', 'chatbot', 'recognition',
    'classification', 'prediction', 'analysis', 'automation'
  ],
  categories: [
    'TEXT_GENERATION', 'IMAGE_GENERATION', 'AUDIO_GENERATION',
    'VIDEO_GENERATION', 'DATA_ANALYSIS', 'AUTOMATION'
  ],
  tags: [
    'popular', 'trending', 'new', 'recommended', 'free', 'premium'
  ],
  sortOptions: ['LATEST', 'POPULAR', 'RATING', 'DOWNLOAD_COUNT'],
  pricingOptions: [true, false, null] // 무료, 유료, 전체
};

// 테스트용 사용자 계정
const testUser = {
  email: 'test@nomodel.com',
  password: 'password123'
};

// 로그인해서 얻은 쿠키를 저장할 변수
let sessionCookies = '';

// 로그인 함수
function login() {
  const loginPayload = {
    email: testUser.email,
    password: testUser.password
  };

  const loginResponse = http.post(`${BASE_URL}/auth/login`, JSON.stringify(loginPayload), {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (loginResponse.status === 200) {
    // 응답 헤더에서 Set-Cookie 값들을 추출
    const cookies = loginResponse.headers['Set-Cookie'];

    if (cookies) {
      // Set-Cookie 배열에서 쿠키 이름=값만 추출
      const cookieValues = [];
      const cookieArray = Array.isArray(cookies) ? cookies : [cookies];

      cookieArray.forEach(cookie => {
        // 쿠키에서 이름=값 부분만 추출 (첫 번째 세미콜론 이전)
        const cookieValue = cookie.split(';')[0].trim();
        cookieValues.push(cookieValue);
      });

      sessionCookies = cookieValues.join('; ');
    }
  }

  return loginResponse.status === 200;
}

// 메인 테스트 함수
export default function() {
  const startTime = Date.now();

  // 세션 쿠키가 없으면 로그인 수행
  if (!sessionCookies) {
    login();
  }

  // 현재 활성 검색 사용자 수 업데이트
  activeSearchUsers.add(1);

  // 검색 테스트 시나리오 실행
  testUnifiedSearch();
  testAdminModelSearch();
  testUserModelSearch();
  testSearchWithVariousFilters();
  testPagination();
  testSortingOptions();

  // 요청 수 증가
  searchRequestCount.add(1);

  // 응답 시간 기록
  const endTime = Date.now();
  searchResponseTime.add(endTime - startTime);

  // 사용자 간 요청 간격
  sleep(Math.random() * 2 + 1); // 1-3초 랜덤 대기
}

// URL 파라미터 빌드 헬퍼 함수
function buildQueryString(params) {
  const entries = Object.entries(params).filter(([key, value]) => value !== null && value !== undefined);
  return entries.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&');
}

// 통합 검색 테스트
function testUnifiedSearch() {
  const keyword = testSearchData.keywords[Math.floor(Math.random() * testSearchData.keywords.length)];
  const isFree = testSearchData.pricingOptions[Math.floor(Math.random() * testSearchData.pricingOptions.length)];
  const sortBy = testSearchData.sortOptions[Math.floor(Math.random() * testSearchData.sortOptions.length)];

  const params = {
    keyword: keyword,
    page: '0',
    size: '20',
    sortBy: sortBy
  };

  if (isFree !== null) {
    params.isFree = isFree.toString();
  }

  const response = http.get(`${BASE_URL}/models/search?${buildQueryString(params)}`);

  const success = check(response, {
    'Unified search status is 200': (r) => r.status === 200,
    'Unified search response time < 300ms': (r) => r.timings.duration < 300,
    'Unified search has valid response structure': (r) => {
      try {
        const data = r.json();
        return data.success && Array.isArray(data.response.content);
      } catch (e) {
        return false;
      }
    },
    'Unified search respects price filter': (r) => {
      if (isFree === null) return true; // 전체 조회시 필터 검증 생략
      try {
        const data = r.json();
        const models = data.response.content;
        if (models.length === 0) return true; // 결과가 없으면 통과

        return models.every(model => {
          const modelIsFree = model.price === 0 || model.price === null;
          return isFree ? modelIsFree : !modelIsFree;
        });
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    searchErrorRate.add(1);
  }
}

// 관리자 모델 검색 테스트
function testAdminModelSearch() {
  const keyword = testSearchData.keywords[Math.floor(Math.random() * testSearchData.keywords.length)];
  const isFree = testSearchData.pricingOptions[Math.floor(Math.random() * testSearchData.pricingOptions.length)];

  const params = {
    keyword: keyword,
    page: '0',
    size: '15'
  };

  if (isFree !== null) {
    params.isFree = isFree.toString();
  }

  const response = http.get(`${BASE_URL}/models/search/admin?${buildQueryString(params)}`);

  const success = check(response, {
    'Admin search status is 200': (r) => r.status === 200,
    'Admin search response time < 300ms': (r) => r.timings.duration < 300,
    'Admin search has valid response': (r) => {
      try {
        const data = r.json();
        return data.success && Array.isArray(data.response.content);
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    searchErrorRate.add(1);
  }
}

// 사용자 모델 검색 테스트 (인증 필요)
function testUserModelSearch() {
  const keyword = testSearchData.keywords[Math.floor(Math.random() * testSearchData.keywords.length)];
  const isFree = testSearchData.pricingOptions[Math.floor(Math.random() * testSearchData.pricingOptions.length)];

  const params = {
    keyword: keyword,
    page: '0',
    size: '10'
  };

  if (isFree !== null) {
    params.isFree = isFree.toString();
  }

  // 인증된 상태로 요청 (쿠키 포함)
  const response = http.get(`${BASE_URL}/models/search/my-models?${buildQueryString(params)}`, {
    headers: {
      'Cookie': sessionCookies
    }
  });

  const success = check(response, {
    'User search status is 200': (r) => r.status === 200,
    'User search response time < 300ms': (r) => r.timings.duration < 300,
    'User search has valid response': (r) => {
      try {
        const data = r.json();
        return data.success && Array.isArray(data.response.content);
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    searchErrorRate.add(1);
  }
}

// 다양한 필터 조합 테스트
function testSearchWithVariousFilters() {
  const keyword = testSearchData.keywords[Math.floor(Math.random() * testSearchData.keywords.length)];
  const category = testSearchData.categories[Math.floor(Math.random() * testSearchData.categories.length)];
  const tag = testSearchData.tags[Math.floor(Math.random() * testSearchData.tags.length)];
  const isFree = testSearchData.pricingOptions[Math.floor(Math.random() * testSearchData.pricingOptions.length)];

  const params = {
    keyword: keyword,
    category: category,
    tags: tag,
    page: '0',
    size: '20'
  };

  if (isFree !== null) {
    params.isFree = isFree.toString();
  }

  const response = http.get(`${BASE_URL}/models/search?${buildQueryString(params)}`);

  const success = check(response, {
    'Complex filter search status is 200': (r) => r.status === 200,
    'Complex filter search response time < 400ms': (r) => r.timings.duration < 400,
    'Complex filter search has results': (r) => {
      try {
        const data = r.json();
        return data.success;
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    searchErrorRate.add(1);
  }
}

// 페이지네이션 테스트
function testPagination() {
  const page = Math.floor(Math.random() * 5); // 0-4 페이지
  const size = [10, 20, 50][Math.floor(Math.random() * 3)];

  const params = {
    page: page.toString(),
    size: size.toString(),
    sortBy: 'LATEST'
  };

  const response = http.get(`${BASE_URL}/models/search?${buildQueryString(params)}`);

  const success = check(response, {
    'Pagination search status is 200': (r) => r.status === 200,
    'Pagination search response time < 250ms': (r) => r.timings.duration < 250,
    'Pagination has correct page info': (r) => {
      try {
        const data = r.json();
        const pageInfo = data.response;
        return pageInfo.hasOwnProperty('totalElements') &&
               pageInfo.hasOwnProperty('totalPages') &&
               pageInfo.hasOwnProperty('page');
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    searchErrorRate.add(1);
  }
}

// 정렬 옵션 테스트
function testSortingOptions() {
  const sortBy = testSearchData.sortOptions[Math.floor(Math.random() * testSearchData.sortOptions.length)];

  const params = {
    page: '0',
    size: '20',
    sortBy: sortBy
  };

  const response = http.get(`${BASE_URL}/models/search?${buildQueryString(params)}`);

  const success = check(response, {
    'Sorting search status is 200': (r) => r.status === 200,
    'Sorting search response time < 300ms': (r) => r.timings.duration < 300,
    'Sorting search has content': (r) => {
      try {
        const data = r.json();
        return data.success && Array.isArray(data.response.content);
      } catch (e) {
        return false;
      }
    }
  });

  if (!success) {
    searchErrorRate.add(1);
  }
}

// 테스트 시작 로그
console.log(`🔍 Starting AI Model Search ${TEST_TYPE.toUpperCase()} test`);
console.log(`📊 Test type: ${TEST_TYPE}, Stages: ${selectedOptions.stages.length}`);