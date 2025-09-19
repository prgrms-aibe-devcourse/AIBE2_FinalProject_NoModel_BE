// 공통 검증 로직

import { check } from 'k6';

// 기본 HTTP 응답 검증
export function checkBasicResponse(response, expectedStatus = 200, maxDuration = 1000) {
  return check(response, {
    [`Status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Response has body': (r) => r.body.length > 0
  });
}

// API 성공 응답 검증
export function checkApiSuccess(response, maxDuration = 1000) {
  return check(response, {
    'Status is 200': (r) => r.status === 200,
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Response is valid JSON': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    },
    'Response has success=true': (r) => {
      try {
        const data = r.json();
        return data.success === true;
      } catch (e) {
        return false;
      }
    }
  });
}

// 페이지네이션 응답 검증
export function checkPaginationResponse(response, maxDuration = 1000) {
  const basicCheck = checkApiSuccess(response, maxDuration);

  const paginationCheck = check(response, {
    'Has pagination info': (r) => {
      try {
        const data = r.json();
        const pageData = data.data;
        return pageData.hasOwnProperty('content') &&
               pageData.hasOwnProperty('totalElements') &&
               pageData.hasOwnProperty('totalPages') &&
               pageData.hasOwnProperty('number') &&
               pageData.hasOwnProperty('size');
      } catch (e) {
        return false;
      }
    },
    'Content is array': (r) => {
      try {
        const data = r.json();
        return Array.isArray(data.data.content);
      } catch (e) {
        return false;
      }
    },
    'Page number is valid': (r) => {
      try {
        const data = r.json();
        return typeof data.data.number === 'number' && data.data.number >= 0;
      } catch (e) {
        return false;
      }
    }
  });

  return basicCheck && paginationCheck;
}

// AI 모델 검색 응답 검증
export function checkSearchResponse(response, options = {}) {
  const { maxDuration = 300, validatePriceFilter = false, expectedFree = null } = options;

  const basicCheck = checkPaginationResponse(response, maxDuration);

  let priceFilterCheck = true;
  if (validatePriceFilter && expectedFree !== null) {
    priceFilterCheck = check(response, {
      'Price filter is correctly applied': (r) => {
        try {
          const data = r.json();
          const models = data.data.content;

          if (models.length === 0) return true; // 결과가 없으면 통과

          return models.every(model => {
            const modelIsFree = model.price === 0 || model.price === null || model.price === undefined;
            return expectedFree ? modelIsFree : !modelIsFree;
          });
        } catch (e) {
          console.error('Price filter validation error:', e);
          return false;
        }
      }
    });
  }

  return basicCheck && priceFilterCheck;
}

// 에러 응답 검증
export function checkErrorResponse(response, expectedStatus, maxDuration = 1000) {
  return check(response, {
    [`Status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Response has error message': (r) => {
      try {
        const data = r.json();
        return data.success === false && data.error;
      } catch (e) {
        return false;
      }
    }
  });
}

// 인증 관련 검증
export function checkAuthResponse(response, maxDuration = 1000) {
  return check(response, {
    'Auth status is 200 or 401': (r) => [200, 401].includes(r.status),
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Valid auth response': (r) => {
      if (r.status === 401) {
        // 401인 경우 에러 응답 구조 검증
        try {
          const data = r.json();
          return data.success === false;
        } catch (e) {
          return false;
        }
      } else {
        // 200인 경우 성공 응답 구조 검증
        try {
          const data = r.json();
          return data.success === true;
        } catch (e) {
          return false;
        }
      }
    }
  });
}

// 헬스 체크 검증
export function checkHealthResponse(response, maxDuration = 200) {
  return check(response, {
    'Health check status is 200': (r) => r.status === 200,
    [`Health check response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Health status is UP': (r) => {
      try {
        const data = r.json();
        return data.status === 'UP';
      } catch (e) {
        return false;
      }
    }
  });
}

// 검색 키워드 유효성 검증
export function validateSearchKeyword(keyword) {
  if (!keyword || typeof keyword !== 'string') return false;
  if (keyword.length < 1 || keyword.length > 100) return false;

  // SQL Injection 방지를 위한 기본 검증
  const dangerousPatterns = [
    /['"`;]/,           // 따옴표, 세미콜론
    /\b(DROP|DELETE|UPDATE|INSERT|SELECT)\b/i,  // SQL 키워드
    /<script/i,         // XSS 방지
    /javascript:/i      // 자바스크립트 프로토콜
  ];

  return !dangerousPatterns.some(pattern => pattern.test(keyword));
}

// 페이지네이션 파라미터 유효성 검증
export function validatePaginationParams(page, size) {
  if (typeof page !== 'number' || page < 0) return false;
  if (typeof size !== 'number' || size < 1 || size > 100) return false;

  // Elasticsearch의 기본 max_result_window는 10000
  const maxOffset = page * size;
  return maxOffset < 10000;
}

// 성능 메트릭 검증 헬퍼
export function checkPerformanceThresholds(response, thresholds) {
  const { response_time_p95, response_time_p99, max_duration } = thresholds;

  return check(response, {
    [`Response time < ${max_duration || response_time_p95}ms`]: (r) =>
      r.timings.duration < (max_duration || response_time_p95),
    'Response is successful': (r) => r.status === 200,
    'Response has content': (r) => r.body.length > 0
  });
}