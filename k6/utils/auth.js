import http from 'k6/http';

// 기본 API URL
const BASE_URL = 'http://host.docker.internal:8080/api';

// 테스트용 사용자 계정 설정 (@nomodel.com 도메인 사용)
const DEFAULT_TEST_USERS = {
  normal: {
    email: 'test@nomodel.com',
    password: 'password123'
  },
  admin: {
    email: 'admin@nomodel.com',
    password: 'password123'
  },
  premium: {
    email: 'premium@nomodel.com',
    password: 'password123'
  }
};

// 세션 쿠키 저장소 (사용자별)
const sessionCookies = new Map();

/**
 * 로그인 함수 - JWT 토큰을 포함한 쿠키 획득
 * @param {Object} user - 사용자 정보 { email, password }
 * @param {string} baseUrl - API 기본 URL (선택적)
 * @returns {boolean} 로그인 성공 여부
 */
export function login(user = DEFAULT_TEST_USERS.normal, baseUrl = BASE_URL) {
  const userKey = user.email;

  // 이미 로그인된 사용자인지 확인
  if (sessionCookies.has(userKey)) {
    return true;
  }

  const loginPayload = {
    email: user.email,
    password: user.password
  };

  const loginResponse = http.post(`${baseUrl}/auth/login`, JSON.stringify(loginPayload), {
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

      const cookieString = cookieValues.join('; ');
      sessionCookies.set(userKey, cookieString);
    }
  }

  return loginResponse.status === 200;
}

/**
 * 저장된 세션 쿠키 가져오기
 * @param {Object} user - 사용자 정보
 * @returns {string} 쿠키 문자열
 */
export function getSessionCookies(user = DEFAULT_TEST_USERS.normal) {
  return sessionCookies.get(user.email) || '';
}

/**
 * 인증이 필요한 HTTP 요청을 위한 헤더 생성
 * @param {Object} user - 사용자 정보
 * @param {Object} additionalHeaders - 추가 헤더 (선택적)
 * @returns {Object} HTTP 헤더 객체
 */
export function getAuthHeaders(user = DEFAULT_TEST_USERS.normal, additionalHeaders = {}) {
  const cookies = getSessionCookies(user);

  return {
    'Cookie': cookies,
    ...additionalHeaders
  };
}

/**
 * 인증된 GET 요청
 * @param {string} url - 요청 URL
 * @param {Object} user - 사용자 정보 (선택적)
 * @param {Object} params - 요청 파라미터 (선택적)
 * @returns {Object} HTTP 응답
 */
export function authenticatedGet(url, user = DEFAULT_TEST_USERS.normal, params = {}) {
  // 사용자가 로그인되어 있지 않으면 로그인 수행
  if (!sessionCookies.has(user.email)) {
    login(user);
  }

  return http.get(url, {
    headers: getAuthHeaders(user),
    ...params
  });
}

/**
 * 인증된 POST 요청
 * @param {string} url - 요청 URL
 * @param {Object} body - 요청 본문
 * @param {Object} user - 사용자 정보 (선택적)
 * @param {Object} params - 요청 파라미터 (선택적)
 * @returns {Object} HTTP 응답
 */
export function authenticatedPost(url, body, user = DEFAULT_TEST_USERS.normal, params = {}) {
  // 사용자가 로그인되어 있지 않으면 로그인 수행
  if (!sessionCookies.has(user.email)) {
    login(user);
  }

  const headers = getAuthHeaders(user, {
    'Content-Type': 'application/json'
  });

  return http.post(url, JSON.stringify(body), {
    headers: headers,
    ...params
  });
}

/**
 * 인증된 PUT 요청
 * @param {string} url - 요청 URL
 * @param {Object} body - 요청 본문
 * @param {Object} user - 사용자 정보 (선택적)
 * @param {Object} params - 요청 파라미터 (선택적)
 * @returns {Object} HTTP 응답
 */
export function authenticatedPut(url, body, user = DEFAULT_TEST_USERS.normal, params = {}) {
  // 사용자가 로그인되어 있지 않으면 로그인 수행
  if (!sessionCookies.has(user.email)) {
    login(user);
  }

  const headers = getAuthHeaders(user, {
    'Content-Type': 'application/json'
  });

  return http.put(url, JSON.stringify(body), {
    headers: headers,
    ...params
  });
}

/**
 * 인증된 DELETE 요청
 * @param {string} url - 요청 URL
 * @param {Object} user - 사용자 정보 (선택적)
 * @param {Object} params - 요청 파라미터 (선택적)
 * @returns {Object} HTTP 응답
 */
export function authenticatedDelete(url, user = DEFAULT_TEST_USERS.normal, params = {}) {
  // 사용자가 로그인되어 있지 않으면 로그인 수행
  if (!sessionCookies.has(user.email)) {
    login(user);
  }

  return http.del(url, null, {
    headers: getAuthHeaders(user),
    ...params
  });
}

/**
 * 모든 세션 쿠키 초기화
 */
export function clearAllSessions() {
  sessionCookies.clear();
}

/**
 * 특정 사용자 세션 초기화
 * @param {Object} user - 사용자 정보
 */
export function clearUserSession(user) {
  sessionCookies.delete(user.email);
}

/**
 * 사전 정의된 테스트 사용자 가져오기
 */
export function getTestUsers() {
  return DEFAULT_TEST_USERS;
}

/**
 * 로그인 상태 확인
 * @param {Object} user - 사용자 정보
 * @returns {boolean} 로그인 여부
 */
export function isLoggedIn(user) {
  return sessionCookies.has(user.email);
}