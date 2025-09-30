import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  checkApiSuccess,
  checkHealthResponse,
  checkSearchResponse
} from '../utils/common-checks.js';
import { generateSearchParams, getRandomSuggestionPrefix } from '../utils/test-data.js';

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<300'],
    http_req_failed: ['rate<0.02']
  }
};

const BASE_URL = (__ENV.K6_BASE_URL || 'http://host.docker.internal:8080/api');

export default function () {
  // Actuator health
  let response = http.get(`${BASE_URL}/actuator/health`);
  checkHealthResponse(response, 200);

  // 기본 모델 검색
  const searchParams = generateSearchParams({ page: 0, size: 5 });
  response = http.get(`${BASE_URL}/models/search?${searchParams.toString()}`);
  checkSearchResponse(response, { maxDuration: 400 });

  // 관리자 모델 검색
  response = http.get(`${BASE_URL}/models/search/admin?page=0&size=5`);
  checkSearchResponse(response, { maxDuration: 450 });

  // 자동완성 제안
  const prefix = getRandomSuggestionPrefix();
  response = http.get(`${BASE_URL}/models/search/suggestions?prefix=${encodeURIComponent(prefix)}`);
  checkApiSuccess(response, 300);
  check(response, {
    'Suggestions payload is array': (r) => {
      try {
        const payload = r.json();
        return Array.isArray(payload.response);
      } catch (e) {
        return false;
      }
    }
  });

  sleep(1);
}

console.log('🚀 Starting Smoke Test - Model search baseline verification');
