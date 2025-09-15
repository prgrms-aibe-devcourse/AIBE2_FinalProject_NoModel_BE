package com.example.nomodel._core.base;

import com.example.nomodel._core.config.TestContainersConfig;
import com.example.nomodel._core.config.TestFirebaseConfig;
import com.example.nomodel._core.config.TestOAuth2Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 통합 테스트를 위한 베이스 클래스
 * TestContainers를 사용하여 실제 데이터베이스, Redis, Elasticsearch와 연동합니다.
 *
 * 특징:
 * - MySQL TestContainer 사용 (실제 DB)
 * - Redis TestContainer 사용 (실제 Redis)
 * - Elasticsearch TestContainer 사용 (실제 ES)
 * - 실제 외부 시스템과 동일한 환경
 * - 느리지만 정확한 테스트
 *
 * 사용 시나리오:
 * - 실제 외부 시스템과의 통합 테스트
 * - 데이터베이스 트랜잭션 테스트
 * - Redis 캐싱 로직 테스트
 * - Elasticsearch 검색 기능 테스트
 * - 전체 시스템 통합 테스트
 *
 * 예시:
 * ```java
 * class FullSystemIntegrationTest extends BaseIntegrationTest {
 *     // 실제 시스템과 동일한 환경에서 테스트
 * }
 * ```
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("itest")
@Import({TestContainersConfig.class, TestFirebaseConfig.class, TestOAuth2Config.class})
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}