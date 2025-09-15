package com.example.nomodel._core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

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

    // ElasticSearch는 대부분의 통합 테스트에서 불필요하므로 제거
    // 필요한 경우 별도 테스트 클래스에서 @MockBean ElasticsearchOperations 사용

}