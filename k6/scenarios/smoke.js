import http from 'k6/http';
import { check, sleep } from 'k6';
import { checkHealthResponse, checkBasicResponse } from '../utils/common-checks.js';

// 스모크 테스트: 기본 기능 검증
export const options = {
  vus: 1,        // 1명의 가상 사용자
  duration: '1m', // 1분 동안
  thresholds: {
    http_req_duration: ['p(95)<200'],  // 95% 요청이 200ms 미만
    http_req_failed: ['rate<0.01'],    // 실패율 1% 미만
  },
};

const BASE_URL = 'http://host.docker.internal:8080/api';

export default function() {
  // 헬스 체크
  let response = http.get(`${BASE_URL}/actuator/health`);
  checkHealthResponse(response, 200);

  // 기본 API 엔드포인트 체크
  response = http.get(`${BASE_URL}/health`);
  check(response, {
    'API health endpoint responds': (r) => r.status === 200 || r.status === 404,
  });

  // AI 모델 검색 API 기본 동작 확인
  response = http.get(`${BASE_URL}/models/search?page=0&size=1`);
  check(response, {
    'AI model search endpoint responds': (r) => r.status === 200,
    'AI model search response time < 300ms': (r) => r.timings.duration < 300,
    'AI model search returns JSON': (r) => {
      try {
        const data = r.json();
        return data.success !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  // 정적 리소스 체크
  response = http.get(`http://host.docker.internal:8080/`);
  check(response, {
    'Root endpoint responds': (r) => r.status === 200 || r.status === 404,
  });

  sleep(1);
}

console.log('🚀 Starting Smoke Test - Basic functionality verification');