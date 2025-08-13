import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// 커스텀 메트릭 정의
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time');
const activeUsers = new Gauge('active_users');
const requestCount = new Counter('request_count');

// 환경 변수에 따른 테스트 설정 선택
const TEST_TYPE = __ENV.TEST_TYPE || 'load';

// 기본 로드 테스트 설정
const loadTestOptions = {
  stages: [
    { duration: '30s', target: 10 },  // 워밍업: 30초 동안 10명까지
    { duration: '1m', target: 20 },   // 부하 증가: 1분 동안 20명까지
    { duration: '2m', target: 50 },   // 피크 부하: 2분 동안 50명 유지
    { duration: '1m', target: 20 },   // 부하 감소: 1분 동안 20명까지 감소
    { duration: '30s', target: 0 },   // 쿨다운: 30초 동안 0명까지
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95%는 500ms 미만, 99%는 1초 미만
    http_req_failed: ['rate<0.05'],                  // 실패율 5% 미만
    error_rate: ['rate<0.05'],                       // 커스텀 에러율 5% 미만
    response_time: ['p(95)<500'],                    // 응답시간 95% 500ms 미만
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
};

// 테스트 설정 선택 로직
let selectedOptions;
switch (TEST_TYPE) {
  case 'stress':
    selectedOptions = {
      stages: [
        { duration: '5m', target: 100 },   // 5분 동안 100명까지
        { duration: '10m', target: 200 },  // 10분 동안 200명까지
        { duration: '5m', target: 300 },   // 5분 동안 300명까지
        { duration: '10m', target: 200 },  // 10분 동안 200명으로 감소
        { duration: '5m', target: 0 },     // 5분 동안 0명까지
      ],
      thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        http_req_failed: ['rate<0.10'],
        error_rate: ['rate<0.10'],
      },
      summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    };
    break;
  case 'spike':
    selectedOptions = {
      stages: [
        { duration: '30s', target: 10 },   // 워밍업
        { duration: '1m', target: 500 },   // 급격한 부하 증가
        { duration: '30s', target: 10 },   // 급격한 부하 감소
      ],
      thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.15'],
      },
      summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
    };
    break;
  default: // 'load'
    selectedOptions = loadTestOptions;
}

export const options = selectedOptions;

// 기본 URL 설정
const BASE_URL = 'http://host.docker.internal:8080';

// 테스트 데이터
const testData = {
  users: [
    { username: 'testuser1', email: 'test1@example.com' },
    { username: 'testuser2', email: 'test2@example.com' },
    { username: 'testuser3', email: 'test3@example.com' },
  ],
  products: [
    { name: 'Product A', price: 10000 },
    { name: 'Product B', price: 20000 },
    { name: 'Product C', price: 30000 },
  ]
};

// 메인 테스트 함수
export default function() {
  const startTime = Date.now();
  
  // 현재 활성 사용자 수 업데이트
  activeUsers.add(1);
  
  // 테스트 시나리오 실행
  testHealthCheck();
  testUserRegistration();
  testProductCatalog();
  testDatabaseOperations();
  
  // 요청 수 증가
  requestCount.add(1);
  
  // 응답 시간 기록
  const endTime = Date.now();
  responseTime.add(endTime - startTime);
  
  // 사용자 간 요청 간격
  sleep(Math.random() * 3 + 1); // 1-4초 랜덤 대기
}

// 헬스 체크 테스트
function testHealthCheck() {
  const response = http.get(`${BASE_URL}/actuator/health`);
  
  const success = check(response, {
    'Health check status is 200': (r) => r.status === 200,
    'Health check response time < 200ms': (r) => r.timings.duration < 200,
    'Health status is UP': (r) => r.json('status') === 'UP',
  });
  
  if (!success) {
    errorRate.add(1);
  }
}

// 사용자 등록 테스트
function testUserRegistration() {
  const user = testData.users[Math.floor(Math.random() * testData.users.length)];
  
  const payload = JSON.stringify({
    username: `${user.username}_${Math.random().toString(36).substr(2, 9)}`,
    email: user.email,
    password: 'testpass123'
  });
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const response = http.post(`${BASE_URL}/api/users/register`, payload, params);
  
  const success = check(response, {
    'User registration status is 201 or 409': (r) => [201, 409].includes(r.status),
    'User registration response time < 500ms': (r) => r.timings.duration < 500,
  });
  
  if (!success) {
    errorRate.add(1);
  }
}

// 상품 카탈로그 테스트
function testProductCatalog() {
  // 상품 목록 조회
  const listResponse = http.get(`${BASE_URL}/api/products`);
  
  const listSuccess = check(listResponse, {
    'Product list status is 200': (r) => r.status === 200,
    'Product list response time < 300ms': (r) => r.timings.duration < 300,
  });
  
  if (!listSuccess) {
    errorRate.add(1);
    return;
  }
  
  // 상품 검색 테스트
  const searchResponse = http.get(`${BASE_URL}/api/products/search?q=Product`);
  
  const searchSuccess = check(searchResponse, {
    'Product search status is 200': (r) => r.status === 200,
    'Product search response time < 400ms': (r) => r.timings.duration < 400,
  });
  
  if (!searchSuccess) {
    errorRate.add(1);
  }
}

// 데이터베이스 연산 테스트
function testDatabaseOperations() {
  // 복잡한 쿼리 시뮬레이션
  const response = http.get(`${BASE_URL}/api/reports/summary`);
  
  const success = check(response, {
    'Database query status is 200 or 404': (r) => [200, 404].includes(r.status),
    'Database query response time < 1000ms': (r) => r.timings.duration < 1000,
  });
  
  if (!success) {
    errorRate.add(1);
  }
}

// 테스트 시작 로그
console.log(`🚀 Starting ${TEST_TYPE.toUpperCase()} test with ${options.stages ? options.stages.length : 0} stages`);
console.log(`📊 Test configuration: ${JSON.stringify(options, null, 2)}`);