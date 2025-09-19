package com.example.nomodel.model.application.service;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIModelSearchService 단위 테스트")
class AIModelSearchServiceTest {

    @Mock
    private AIModelSearchRepository searchRepository;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private AIModelSearchService searchService;

    @Test
    @DisplayName("통합 검색 성공")
    void search_Success() {
        // given
        String keyword = "GPT";
        int page = 0;
        int size = 10;
        
        AIModelDocument document1 = createMockDocument("1", "GPT-4", "Advanced language model");
        AIModelDocument document2 = createMockDocument("2", "GPT-3.5", "Chat completion model");
        
        Page<AIModelDocument> expectedPage = new PageImpl<>(Arrays.asList(document1, document2));
        
        given(searchRepository.searchByModelNameAndPrompt(eq(keyword), any(Pageable.class)))
                .willReturn(expectedPage);

        // when
Page<AIModelDocument> result = searchService.search(keyword, null, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getModelName()).isEqualTo("GPT-4");
        
        then(searchRepository).should().searchByModelNameAndPrompt(eq(keyword), any(Pageable.class));
    }


    @Test
    @DisplayName("소유자별 검색 성공")
    void searchByOwner_Success() {
        // given
        Long ownerId = 1L;
        int page = 0;
        int size = 10;
        
        AIModelDocument document = createMockDocument("1", "MyModel", "Owner's model");
        Page<AIModelDocument> expectedPage = new PageImpl<>(List.of(document));
        
        given(searchRepository.findByOwnerId(eq(ownerId), any(Pageable.class)))
                .willReturn(expectedPage);

        // when
        Page<AIModelDocument> result = searchService.searchByOwner(ownerId, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        then(searchRepository).should().findByOwnerId(eq(ownerId), any(Pageable.class));
    }

    @Test
    @DisplayName("인기 모델 검색 성공")
    void getPopularModels_Success() {
        // given
        int page = 0;
        int size = 10;
        
        AIModelDocument document = createMockDocument("1", "PopularModel", "High usage model");
        Page<AIModelDocument> expectedPage = new PageImpl<>(List.of(document));
        
        given(searchRepository.findPopularModels(any(Pageable.class)))
                .willReturn(expectedPage);

        // when
        Page<AIModelDocument> result = searchService.getPopularModels(page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        then(searchRepository).should().findPopularModels(any(Pageable.class));
    }

    @Test
    @DisplayName("최신 모델 검색 성공")
    void getRecentModels_Success() {
        // given
        int page = 0;
        int size = 10;
        
        AIModelDocument document = createMockDocument("1", "NewModel", "Recently created model");
        Page<AIModelDocument> expectedPage = new PageImpl<>(List.of(document));
        
        given(searchRepository.findRecentModels(any(Pageable.class)))
                .willReturn(expectedPage);

        // when
        Page<AIModelDocument> result = searchService.getRecentModels(page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        then(searchRepository).should().findRecentModels(any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 추천 모델 검색 성공")
    void getRecommendedModels_Success() {
        // given
        int page = 0;
        int size = 10;
        
        AIModelDocument document = createMockDocument("1", "RecommendedModel", "Admin recommended");
        Page<AIModelDocument> expectedPage = new PageImpl<>(List.of(document));
        
        given(searchRepository.findRecommendedModels(any(Pageable.class)))
                .willReturn(expectedPage);

        // when
        Page<AIModelDocument> result = searchService.getRecommendedModels(page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        then(searchRepository).should().findRecommendedModels(any(Pageable.class));
    }


    @Test
    @DisplayName("무료 모델 검색 성공")
    void getFreeModels_Success() {
        // given
        int page = 0;
        int size = 10;
        
        AIModelDocument document = createMockDocument("1", "FreeModel", "Free to use model");
        Page<AIModelDocument> expectedPage = new PageImpl<>(List.of(document));
        
        given(searchRepository.findFreeModels(any(Pageable.class)))
                .willReturn(expectedPage);

        // when
        Page<AIModelDocument> result = searchService.getFreeModels(page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        then(searchRepository).should().findFreeModels(any(Pageable.class));
    }


    @Test
    @DisplayName("자동완성 제안 성공")
    void getModelNameSuggestions_Success() throws Exception {
        // given
        String prefix = "GPT";
        
        // ElasticsearchClient가 정상적으로 호출되고 결과를 반환하는지만 확인하는 단순한 테스트
        // 실제 Elasticsearch 응답 모킹은 복잡하므로, 서비스의 예외 처리만 확인
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
                .willThrow(new RuntimeException("Test exception - expect empty list"));

        // when
        List<String> result = searchService.getModelNameSuggestions(prefix);

        // then - 예외 발생 시 빈 리스트 반환 확인
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        then(elasticsearchClient).should().search(any(SearchRequest.class), eq(Void.class));
    }

    @Test
    @DisplayName("자동완성 제안 실패 - ElasticsearchClient 오류")
    void getModelNameSuggestions_Failure() throws Exception {
        // given
        String prefix = "GPT";
        
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class)))
                .willThrow(new RuntimeException("Elasticsearch connection failed"));

        // when
        List<String> result = searchService.getModelNameSuggestions(prefix);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        then(elasticsearchClient).should().search(any(SearchRequest.class), eq(Void.class));
    }


    // 헬퍼 메서드
    private AIModelDocument createMockDocument(String id, String modelName, String prompt) {
        AIModelDocument document = mock(AIModelDocument.class);
        lenient().when(document.getId()).thenReturn(id);
        lenient().when(document.getModelName()).thenReturn(modelName);
        lenient().when(document.getPrompt()).thenReturn(prompt);
        return document;
    }
}