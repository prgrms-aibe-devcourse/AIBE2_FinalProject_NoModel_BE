import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';
import exec from 'k6/execution';
import {
  generateSearchParams,
  getRandomKeyword,
  getRandomPricingOption,
  getRandomSuggestionPrefix
} from '../utils/test-data.js';
import { checkApiSuccess, checkHealthResponse, checkSearchResponse } from '../utils/common-checks.js';

// 커스텀 메트릭 정의
const errorRate = new Rate('error_rate');
const searchResponseTime = new Trend('search_response_time');
const searchRequestCount = new Counter('search_request_count');
const activeSearchUsers = new Gauge('active_search_users');

// 환경 변수에 따른 테스트 설정 선택
const TEST_TYPE = __ENV.TEST_TYPE || 'load';

// 기본 로드 테스트 설정
const loadTestOptions = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 25 },
    { duration: '2m', target: 60 },
    { duration: '1m', target: 25 },
    { duration: '30s', target: 0 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<900'],
    http_req_failed: ['rate<0.05'],
    error_rate: ['rate<0.05'],
    search_response_time: ['p(95)<500']
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)']
};

const stressTestOptions = {
  stages: [
    { duration: '3m', target: 80 },
    { duration: '5m', target: 160 },
    { duration: '3m', target: 240 },
    { duration: '5m', target: 120 },
    { duration: '2m', target: 0 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<1200'],
    http_req_failed: ['rate<0.10'],
    error_rate: ['rate<0.10']
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)']
};

const spikeTestOptions = {
  stages: [
    { duration: '20s', target: 0 },
    { duration: '20s', target: 200 },
    { duration: '20s', target: 0 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.15'],
    error_rate: ['rate<0.15']
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)']
};

let selectedOptions;
switch (TEST_TYPE) {
  case 'stress':
    selectedOptions = stressTestOptions;
    break;
  case 'spike':
    selectedOptions = spikeTestOptions;
    break;
  default:
    selectedOptions = loadTestOptions;
}

export const options = selectedOptions;

const BASE_URL = (__ENV.K6_BASE_URL || 'http://host.docker.internal:8080/api');

export default function () {
  testHealthCheck();
  testPublicModelSearch();
  testFilteredModelSearch();
  testAdminModelSearch();
  testSearchSuggestions();

  sleep(Math.random() * 2 + 1);
}

function recordMetrics(response, success) {
  searchRequestCount.add(1);
  searchResponseTime.add(response.timings.duration);
  activeSearchUsers.add(exec.instance.vusActive);
  if (!success) {
    errorRate.add(1);
  }
}

function testHealthCheck() {
  const response = http.get(`${BASE_URL}/actuator/health`);
  const success = checkHealthResponse(response, 200);
  recordMetrics(response, success);
}

function testPublicModelSearch() {
  const params = generateSearchParams({ page: 0, size: 20 });
  const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);
  const success = checkSearchResponse(response, { maxDuration: 400 });
  recordMetrics(response, success);
}

function testFilteredModelSearch() {
  const isFree = Math.random() > 0.5;
  const params = generateSearchParams({ keyword: false, isFree, page: 0, size: 12 });
  const response = http.get(`${BASE_URL}/models/search?${params.toString()}`);
  const success = checkSearchResponse(response, {
    maxDuration: 450,
    validatePriceFilter: true,
    expectedFree: isFree
  });
  recordMetrics(response, success);
}

function testAdminModelSearch() {
  const isFree = getRandomPricingOption();
  const params = generateSearchParams({ keyword: false, isFree, page: 0, size: 20 });
  const response = http.get(`${BASE_URL}/models/search/admin?${params.toString()}`);
  const success = checkSearchResponse(response, { maxDuration: 500 });
  recordMetrics(response, success);
}

function testSearchSuggestions() {
  const baseKeyword = getRandomKeyword();
  const prefix = baseKeyword.substring(0, 3) || getRandomSuggestionPrefix();
  const response = http.get(`${BASE_URL}/models/search/suggestions?prefix=${encodeURIComponent(prefix)}`);

  const success = checkApiSuccess(response, 300) &&
    check(response, {
      'Suggestions payload is array': (r) => {
        try {
          const data = r.json();
          return Array.isArray(data.response);
        } catch (e) {
          return false;
        }
      }
  });

  recordMetrics(response, success);
}

console.log(`🚀 Starting ${TEST_TYPE.toUpperCase()} model-search test`);
console.log(`📊 Test configuration: ${JSON.stringify(options, null, 2)}`);
