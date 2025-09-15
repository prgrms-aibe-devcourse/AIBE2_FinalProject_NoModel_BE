package com.example.nomodel._core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;

/**
 * TestContainers 설정 클래스
 * 실제 통합 테스트(itest 프로파일)를 위한 Docker 컨테이너를 자동으로 관리합니다.
 * Spring Boot 3.1+의 @ServiceConnection을 사용하여 자동 설정합니다.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("itest")
public class TestContainersConfig {

    /**
     * MySQL TestContainer 설정
     * @ServiceConnection이 자동으로 DataSource를 설정합니다.
     */
    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withReuse(true);
    }

    /**
     * Redis TestContainer 설정
     * @ServiceConnection(name = "redis")가 자동으로 Redis 연결을 설정합니다.
     */
    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
    }

    /**
     * Elasticsearch TestContainer 설정
     * 프로젝트 환경과 동일한 버전 사용 (8.18.1)
     * 보안 ON (기본) + 적절한 헬스체크 설정
     */
    @Bean
    @ServiceConnection
    public ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.18.1"))
                // 단일 노드 모드
                .withEnv("discovery.type", "single-node")
                // 보안 OFF → HTTP 모드
                .withEnv("xpack.security.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false")
                .withEnv("xpack.security.transport.ssl.enabled", "false")
                // 메모리 세팅
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                // HTTP 헬스체크 - 더 관대한 설정
                .waitingFor(
                        Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=120s")
                                .forPort(9200)
                                .withStartupTimeout(Duration.ofMinutes(5))
                )
                .withStartupTimeout(Duration.ofMinutes(5))
                // 컨테이너 로깅 활성화
                .withLogConsumer(outputFrame ->
                    System.out.println("ES Container: " + outputFrame.getUtf8String().trim()))
                .withReuse(true);
    }

}