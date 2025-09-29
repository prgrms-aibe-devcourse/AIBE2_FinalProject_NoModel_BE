// 공통 검증 로직

import { check } from 'k6';

function parseJson(response) {
  try {
    return response.json();
  } catch (e) {
    return null;
  }
}

// 기본 HTTP 응답 검증
export function checkBasicResponse(response, expectedStatus = 200, maxDuration = 1000) {
  return check(response, {
    [`Status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Response has body': (r) => r.body && r.body.length > 0
  });
}

// API 성공 응답 검증
export function checkApiSuccess(response, maxDuration = 1000) {
  const payload = parseJson(response);

  return check(response, {
    'Status is 200': (r) => r.status === 200,
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Response is valid JSON': () => payload !== null,
    'Response has success=true': () => payload?.success === true,
    'Response field is present': () => payload?.response !== undefined
  });
}

// 페이지네이션 응답 검증
export function checkPaginationResponse(response, maxDuration = 1000) {
  const payload = parseJson(response);
  const basicCheck = checkApiSuccess(response, maxDuration);

  if (!payload || payload.response === undefined) {
    return false;
  }

  const pageData = payload.response;

  const paginationCheck = check(response, {
    'Has pagination info': () => pageData &&
      Object.prototype.hasOwnProperty.call(pageData, 'content') &&
      Object.prototype.hasOwnProperty.call(pageData, 'totalElements') &&
      Object.prototype.hasOwnProperty.call(pageData, 'totalPages') &&
      Object.prototype.hasOwnProperty.call(pageData, 'number') &&
      Object.prototype.hasOwnProperty.call(pageData, 'size'),
    'Content is array': () => Array.isArray(pageData?.content),
    'Page number is valid': () => typeof pageData?.number === 'number' && pageData.number >= 0
  });

  return basicCheck && paginationCheck;
}

// AI 모델 검색 응답 검증
export function checkSearchResponse(response, options = {}) {
  const { maxDuration = 300, validatePriceFilter = false, expectedFree = null } = options;

  const payload = parseJson(response);
  const pageData = payload?.response;
  const basicCheck = checkPaginationResponse(response, maxDuration);

  let priceFilterCheck = true;
  if (validatePriceFilter && expectedFree !== null && Array.isArray(pageData?.content)) {
    priceFilterCheck = check(response, {
      'Price filter is correctly applied': () => {
        const models = pageData.content;
        if (models.length === 0) {
          return true;
        }

        return models.every((model) => {
          const modelIsFree = model.price === 0 || model.price === null || model.price === undefined;
          return expectedFree ? modelIsFree : !modelIsFree;
        });
      }
    });
  }

  return basicCheck && priceFilterCheck;
}

// 에러 응답 검증
export function checkErrorResponse(response, expectedStatus, maxDuration = 1000) {
  const payload = parseJson(response);

  return check(response, {
    [`Status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Response has error message': () => payload?.success === false && payload?.error !== undefined
  });
}

// 인증 관련 검증
export function checkAuthResponse(response, maxDuration = 1000) {
  const payload = parseJson(response);

  return check(response, {
    'Auth status is 200 or 401': (r) => [200, 401].includes(r.status),
    [`Response time < ${maxDuration}ms`]: (r) => r.timings.duration < maxDuration,
    'Valid auth response': () => {
      if (response.status === 401) {
        return payload?.success === false;
      }
      return payload?.success === true;
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
