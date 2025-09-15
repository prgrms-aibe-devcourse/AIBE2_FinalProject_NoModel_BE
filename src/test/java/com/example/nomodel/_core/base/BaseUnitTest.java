package com.example.nomodel._core.base;

import com.example.nomodel._core.config.TestElasticsearchConfig;
import com.example.nomodel._core.config.TestOAuth2Config;
import com.example.nomodel._core.config.TestRedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단위 테스트를 위한 베이스 클래스
 * Mock 객체와 인메모리 DB를 사용하여 빠른 테스트 실행을 보장합니다.
 *
 * 특징:
 * - H2 인메모리 데이터베이스 사용
 * - Redis Mock 객체 사용 (TestRedisConfig)
 * - Elasticsearch Mock 객체 사용 (TestElasticsearchConfig)
 * - OAuth2 Mock 설정 (TestOAuth2Config)
 * - 외부 의존성 없음
 * - 빠른 실행 속도
 *
 * 사용 시나리오:
 * - API 엔드포인트 단위 테스트 (MockMvc 사용)
 * - 서비스 로직 단위 테스트
 * - 컨트롤러 단위 테스트
 * - 도메인 로직 테스트
 * - 외부 시스템 연동이 필요 없는 모든 테스트
 *
 * 예시:
 * ```java
 * class MemberServiceTest extends BaseUnitTest {
 *     // 빠른 단위 테스트 작성
 * }
 * ```
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestOAuth2Config.class, TestElasticsearchConfig.class, TestRedisConfig.class})
@Transactional
public abstract class BaseUnitTest {

    @Autowired
    protected MockMvc mockMvc;
}