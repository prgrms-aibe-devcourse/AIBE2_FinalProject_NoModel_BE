package com.example.nomodel;

import com.example.nomodel._core.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * 애플리케이션 전체 통합 테스트
 * CI/CD 파이프라인의 integration-test 단계에서 사용
 * TestContainers를 사용하여 실제 환경과 동일한 테스트
 */
@DisplayName("애플리케이션 통합 테스트 (Full)")
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class NoModelApplicationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("실제 환경에서 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인")
    void contextLoadsWithRealDependencies() {
        // MySQL, Redis, Elasticsearch가 모두 실제로 연결된 상태에서
        // 컨텍스트 로드 성공 시 테스트 통과
    }
}