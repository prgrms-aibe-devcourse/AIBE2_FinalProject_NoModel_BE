package com.example.nomodel._core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 설정 클래스
 * Spring Data Elasticsearch와 Elasticsearch 클라이언트 설정을 담당
 * test 프로필에서는 비활성화됨
 */
@Configuration
@Profile("!test")
@EnableElasticsearchRepositories(basePackages = "com.example.nomodel.**.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUrl;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:30s}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private String socketTimeout;

    /**
     * Elasticsearch 클라이언트 설정
     * ELK 스택의 Elasticsearch와 연결하기 위한 클라이언트 구성
     */
    @Override
    public ClientConfiguration clientConfiguration() {
        var builder = ClientConfiguration.builder()
                .connectedTo(extractHostAndPort(elasticsearchUrl))
                .withConnectTimeout(parseDuration(connectionTimeout))
                .withSocketTimeout(parseDuration(socketTimeout));

        // 보안이 활성화된 경우에만 인증 정보 추가
        if (isSecurityEnabled()) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }

    /**
     * URL에서 호스트와 포트 추출
     */
    private String extractHostAndPort(String url) {
        return url.replace("http://", "").replace("https://", "");
    }

    /**
     * Duration 문자열 파싱
     */
    private java.time.Duration parseDuration(String duration) {
        return java.time.Duration.parse("PT" + duration.replace("s", "S"));
    }


    /**
     * 보안 설정 여부 확인
     * 개발환경에서는 ELK Stack 보안이 비활성화되어 있으므로 false 반환
     */
    private boolean isSecurityEnabled() {
        return username != null && !username.isEmpty() && 
               password != null && !password.isEmpty();
    }
}