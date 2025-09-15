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
     * 프로젝트 환경과 동일한 버전 사용 (8.15.0)
     * 보안 OFF + HTTP 모드 + 적절한 헬스체크 설정
     */
    @Bean
    @ServiceConnection
    public ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
                // 단일 노드
                .withEnv("discovery.type", "single-node")
                // 보안/SSL OFF → http 사용
                .withEnv("xpack.security.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false")
                .withEnv("xpack.security.transport.ssl.enabled", "false")
                // 외부 다운로드 비활성화(부팅 안정화)
                .withEnv("ingest.geoip.downloader.enabled", "false")
                // 메모리 (Docker Desktop 메모리 넉넉하면 512m도 OK)
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                // ★ memory_lock 제거 (ulimit 미설정이면 타임아웃 원인)
                // .withEnv("bootstrap.memory_lock", "true")  // 제거
                // 헬스체크(HTTP)
                .waitingFor(
                        Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=180s")
                                .forPort(9200)
                                .withStartupTimeout(Duration.ofMinutes(5))
                )
                .withStartupTimeout(Duration.ofMinutes(5))
                // 재사용 권장(속도/안정성)
                .withReuse(true);
    }

    /**
     * Firebase Emulator Suite TestContainer 설정
     * Storage, Auth, Firestore 등을 로컬에서 에뮬레이션
     */
    @Bean
    public GenericContainer<?> firebaseEmulatorContainer() {
        return new GenericContainer<>(DockerImageName.parse("andreysenov/firebase-tools:latest"))
                .withCommand("firebase", "emulators:start",
                    "--only", "auth,firestore,storage",
                    "--project", "demo-test")
                .withExposedPorts(9099, 8080, 9199, 4000) // auth, firestore, storage, ui
                .withEnv("FIREBASE_PROJECT", "demo-test")
                .withEnv("GCLOUD_PROJECT", "demo-test")
                // 에뮬레이터가 완전히 시작될 때까지 대기
                .waitingFor(
                    Wait.forLogMessage(".*All emulators ready.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3))
                )
                .withStartupTimeout(Duration.ofMinutes(3))
                // 컨테이너 로깅 활성화
                .withLogConsumer(outputFrame ->
                    System.out.println("Firebase Emulator: " + outputFrame.getUtf8String().trim()))
                .withReuse(true);
    }

}