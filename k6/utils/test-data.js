// AI 모델 테스트 데이터 유틸리티

export const searchTestData = {
  keywords: [
    'AI', 'machine learning', 'deep learning', 'neural network',
    'computer vision', 'natural language', 'chatbot', 'recognition',
    'classification', 'prediction', 'automation', 'diffusion', 'GAN'
  ],

  categories: [
    'TEXT_GENERATION', 'IMAGE_GENERATION', 'AUDIO_GENERATION',
    'VIDEO_GENERATION', 'DATA_ANALYSIS', 'AUTOMATION'
  ],

  tags: [
    'popular', 'trending', 'new', 'recommended', 'free', 'premium',
    'enterprise', 'beta', 'stable', 'experimental'
  ],

  sortOptions: ['LATEST', 'POPULAR', 'RATING', 'DOWNLOAD_COUNT'],

  pricingOptions: [true, false, null], // 무료, 유료, 전체

  pageSizes: [10, 20, 30],

  suggestionPrefixes: ['a', 'ai', 'mo', 'mo', 'cla', 'pre', 'rec', 'gen']
};

export const memberTestData = {
  testUsers: [
    { email: 'normalUser@test.com', password: 'password123', role: 'USER' },
    { email: 'premiumUser@test.com', password: 'password123', role: 'USER' },
    { email: 'adminUser@test.com', password: 'password123', role: 'ADMIN' },
    { email: 'activeCreator@test.com', password: 'password123', role: 'USER' }
  ],

  // 실제 환경에서는 실제 JWT 토큰으로 교체
  mockTokens: [
    'Bearer mock-token-user-1',
    'Bearer mock-token-user-2',
    'Bearer mock-token-admin',
    'Bearer mock-token-creator'
  ]
};

export const subscriptionTestData = {
  plans: ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'],

  features: [
    'model_access', 'api_calls', 'storage', 'support',
    'custom_models', 'team_collaboration'
  ]
};

// 랜덤 데이터 생성 헬퍼 함수들
export function getRandomKeyword() {
  return searchTestData.keywords[Math.floor(Math.random() * searchTestData.keywords.length)];
}

export function getRandomPricingOption() {
  return searchTestData.pricingOptions[Math.floor(Math.random() * searchTestData.pricingOptions.length)];
}

export function getRandomPageSize() {
  return searchTestData.pageSizes[Math.floor(Math.random() * searchTestData.pageSizes.length)];
}

export function getRandomPage(maxPage = 5) {
  return Math.floor(Math.random() * maxPage);
}

export function getRandomCategory() {
  return searchTestData.categories[Math.floor(Math.random() * searchTestData.categories.length)];
}

export function getRandomTag() {
  return searchTestData.tags[Math.floor(Math.random() * searchTestData.tags.length)];
}

export function getRandomSortOption() {
  return searchTestData.sortOptions[Math.floor(Math.random() * searchTestData.sortOptions.length)];
}

export function getRandomToken() {
  return memberTestData.mockTokens[Math.floor(Math.random() * memberTestData.mockTokens.length)];
}

// 검색 파라미터 생성기
export function generateSearchParams(options = {}) {
  const params = new URLSearchParams();

  if (options.keyword !== false) {
    const keyword = options.keyword ?? getRandomKeyword();
    params.append('keyword', keyword);
  }

  const isFreeParam = options.hasOwnProperty('isFree') ? options.isFree : getRandomPricingOption();
  if (isFreeParam !== null && isFreeParam !== undefined) {
    params.append('isFree', String(isFreeParam));
  }

  const pageValue = options.page !== undefined ? options.page : getRandomPage();
  params.append('page', String(pageValue));

  const sizeValue = options.size || getRandomPageSize();
  params.append('size', String(sizeValue));

  if (options.category) {
    params.append('category', options.category === true ? getRandomCategory() : String(options.category));
  }

  if (options.tags) {
    const tagValue = options.tags === true ? getRandomTag() : options.tags;
    params.append('tags', String(tagValue));
  }

  if (options.sortBy) {
    params.append('sortBy', String(options.sortBy));
  }

  return params;
}

export function getRandomSuggestionPrefix() {
  const pool = searchTestData.suggestionPrefixes;
  const prefix = pool[Math.floor(Math.random() * pool.length)];
  return prefix || 'a';
}

// 성능 임계값 정의
export const performanceThresholds = {
  search: {
    response_time_p95: 300,   // 95% 응답 시간 300ms 미만
    response_time_p99: 500,   // 99% 응답 시간 500ms 미만
    error_rate: 0.02,         // 에러율 2% 미만
    rps_target: 100           // 초당 요청 수 목표
  },

  filter: {
    response_time_p95: 400,   // 복잡한 필터링은 400ms 허용
    response_time_p99: 600,
    error_rate: 0.02
  },

  pagination: {
    response_time_p95: 250,   // 페이지네이션은 더 빨라야 함
    response_time_p99: 400,
    error_rate: 0.01
  }
};

// Elasticsearch 성능 관련 상수
export const elasticsearchLimits = {
  max_result_window: 10000,  // 기본 max_result_window
  max_page_size: 100,        // 권장 최대 페이지 크기
  deep_pagination_threshold: 1000, // 딥 페이지네이션 임계값
  timeout_ms: 5000          // 검색 타임아웃
};
