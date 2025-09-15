package com.example.nomodel._core.config;

import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * 테스트용 ElasticSearch 설정 클래스
 * test 프로필에서 ElasticSearch 관련 Bean들을 Mock으로 대체
 */
@TestConfiguration
@Profile("test")
public class TestElasticsearchConfig {

    @Bean
    @Primary
    public AIModelSearchRepository aiModelSearchRepository() {
        return Mockito.mock(AIModelSearchRepository.class);
    }

    // 핵심: 테스트용 ElasticsearchTemplate Mock
    @Bean @Primary
    public ElasticsearchTemplate elasticsearchTemplate() {
        return Mockito.mock(ElasticsearchTemplate.class);
    }

    // 같은 Mock 인스턴스를 ElasticsearchOperations 타입으로도 노출
    @Bean @Primary
    public ElasticsearchOperations elasticsearchOperations(ElasticsearchTemplate template) {
        return template; // ElasticsearchTemplate은 ElasticsearchOperations를 구현
    }

    @Bean @Primary
    public ElasticsearchClient elasticsearchClient() {
        return Mockito.mock(ElasticsearchClient.class);
    }
}