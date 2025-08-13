import http from 'k6/http';
import { check, sleep } from 'k6';

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
  check(response, {
    'Health check is successful': (r) => r.status === 200,
    'Health status is UP': (r) => r.json('status') === 'UP',
  });
  
  // 기본 API 엔드포인트 체크
  response = http.get(`${BASE_URL}/health`);
  check(response, {
    'API health endpoint responds': (r) => r.status === 200 || r.status === 404,
  });
  
  // 정적 리소스 체크
  response = http.get(`http://host.docker.internal:8080/`);
  check(response, {
    'Root endpoint responds': (r) => r.status === 200 || r.status === 404,
  });
  
  sleep(1);
}