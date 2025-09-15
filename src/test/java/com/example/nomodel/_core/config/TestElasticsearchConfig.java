package com.example.nomodel._core.config;

import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import org.springframework.boot.test.context.TestConfiguration;
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

    @MockitoBean
    private AIModelSearchRepository aiModelSearchRepository;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ElasticsearchClient elasticsearchClient;
}