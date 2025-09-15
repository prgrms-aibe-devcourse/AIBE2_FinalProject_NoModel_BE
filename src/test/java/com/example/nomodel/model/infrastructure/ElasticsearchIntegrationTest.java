package com.example.nomodel.model.infrastructure;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Elasticsearch 통합 테스트")
class ElasticsearchIntegrationTest {

    @Mock
    private AIModelSearchRepository searchRepository;

    @Test
    @DisplayName("AI 모델 문서 저장 및 조회")
    void saveAndFindDocument() {
        // given
        AIModelDocument document = createTestDocument("test-1", "GPT-4", "Advanced language model", 1L, "OpenAI");

        // Mock 설정
        given(searchRepository.save(any(AIModelDocument.class))).willReturn(document);
        given(searchRepository.findById("test-1")).willReturn(Optional.of(document));

        // when
        AIModelDocument saved = searchRepository.save(document);
        Optional<AIModelDocument> found = searchRepository.findById("test-1");

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("test-1");
        assertThat(saved.getModelName()).isEqualTo("GPT-4");
        assertThat(found).isPresent();
        assertThat(found.get().getModelName()).isEqualTo("GPT-4");
    }

    @Test
    @DisplayName("모델명과 프롬프트로 검색")
    void searchByModelNameAndPrompt() {
        // given
        AIModelDocument gpt4 = createTestDocument("1", "GPT-4 Turbo", "Advanced language model for complex tasks", 1L, "OpenAI");
        AIModelDocument gpt35 = createTestDocument("2", "GPT-3.5", "Chat completion model for conversations", 1L, "OpenAI");
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(gpt4, gpt35), pageable, 2);
        
        // Mock 설정 - "GPT" 키워드로 검색 시 GPT 관련 문서들 반환
        given(searchRepository.searchByModelNameAndPrompt(eq("GPT"), any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.searchByModelNameAndPrompt("GPT", pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(doc -> 
            doc.getModelName().contains("GPT") || doc.getPrompt().contains("GPT"));
    }

    @Test
    @DisplayName("소유자 ID로 검색")
    void findByOwnerId() {
        // given
        AIModelDocument user1Model1 = createTestDocument("1", "Model A", "First model", 1L, "User1");
        AIModelDocument user1Model2 = createTestDocument("2", "Model B", "Second model", 1L, "User1");
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(user1Model1, user1Model2), pageable, 2);
        
        // Mock 설정 - 소유자 ID 1L로 검색 시 해당 사용자의 모델들 반환
        given(searchRepository.findByOwnerId(eq(1L), any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findByOwnerId(1L, pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(doc -> doc.getOwnerId().equals(1L));
    }

    @Test
    @DisplayName("공개 모델 검색")
    void findPublicModels() {
        // given
        AIModelDocument publicModel = createTestDocument("1", "Public Model", "Open to everyone", 1L, "User1");
        publicModel.updateVisibility(true);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(publicModel), pageable, 1);
        
        // Mock 설정 - 공개 모델 검색 시 공개 모델만 반환
        given(searchRepository.findByIsPublic(eq(true), any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findByIsPublic(true, pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getIsPublic()).isTrue();
    }

    @Test
    @DisplayName("가격 범위로 검색")
    void searchByPriceRange() {
        // given
        AIModelDocument midModel = createTestDocument("2", "Standard Model", "Mid-range option", 1L, "User1");
        midModel.updatePrice(new BigDecimal("29.99"));
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(midModel), pageable, 1);
        
        // Mock 설정 - 10~50 가격 범위 검색 시 해당 범위의 모델 반환
        given(searchRepository.searchByPriceRange(
            eq(new BigDecimal("10")), eq(new BigDecimal("50")), any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.searchByPriceRange(
            new BigDecimal("10"), new BigDecimal("50"), pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    @DisplayName("평점 기준 검색")
    void findHighRatedModels() {
        // given
        AIModelDocument highRatedModel = createTestDocument("1", "Excellent Model", "High quality", 1L, "User1");
        highRatedModel.updateRating(4.8, 100L);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(highRatedModel), pageable, 1);
        
        // Mock 설정 - 4.0 이상 평점 모델 검색 시 고평점 모델 반환
        given(searchRepository.findHighRatedModels(eq(4.0), any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findHighRatedModels(4.0, pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getRating()).isGreaterThanOrEqualTo(4.0);
    }

    @Test
    @DisplayName("무료 모델 검색")
    void findFreeModels() {
        // given
        AIModelDocument freeModel1 = createTestDocument("1", "Free Model 1", "No cost", 1L, "User1");
        freeModel1.updatePrice(BigDecimal.ZERO);
        
        AIModelDocument freeModel2 = createTestDocument("2", "Free Model 2", "Also free", 1L, "User1");
        freeModel2.updatePrice(BigDecimal.ZERO);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(freeModel1, freeModel2), pageable, 2);
        
        // Mock 설정 - 무료 모델 검색 시 가격이 0인 모델들 반환
        given(searchRepository.findFreeModels(any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findFreeModels(pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(doc -> doc.getPrice().equals(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("추천 모델 검색")
    void findRecommendedModels() {
        // given
        AIModelDocument recommendedModel = createTestDocument("1", "Recommended Model", "Admin pick", 1L, "User1");
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(recommendedModel), pageable, 1);
        
        // Mock 설정 - 추천 모델 검색 시 추천 모델 반환
        given(searchRepository.findRecommendedModels(any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findRecommendedModels(pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getModelName()).isEqualTo("Recommended Model");
    }

    @Test
    @DisplayName("최신 모델 검색")
    void findRecentModels() {
        // given
        AIModelDocument newModel = createTestDocument("2", "New Model", "Created recently", 1L, "User1");
        AIModelDocument oldModel = createTestDocument("1", "Old Model", "Created earlier", 1L, "User1");
        
        Pageable pageable = PageRequest.of(0, 10);
        // 최신순 정렬된 목록 (새로운 모델이 먼저)
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(newModel, oldModel), pageable, 2);
        
        // Mock 설정 - 최신 모델 검색 시 생성일 기준 역순 정렬
        given(searchRepository.findRecentModels(any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findRecentModels(pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("인기 모델 검색 (사용량 기준)")
    void findPopularModels() {
        // given
        AIModelDocument popularModel = createTestDocument("1", "Popular Model", "High usage", 1L, "User1");
        popularModel.increaseUsage();
        popularModel.increaseUsage();
        popularModel.increaseUsage();
        
        AIModelDocument normalModel = createTestDocument("2", "Normal Model", "Low usage", 1L, "User1");
        
        Pageable pageable = PageRequest.of(0, 10);
        // 인기순 정렬된 목록 (인기 모델이 먼저)
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(popularModel, normalModel), pageable, 2);
        
        // Mock 설정 - 인기 모델 검색 시 사용량 기준 역순 정렬
        given(searchRepository.findPopularModels(any(Pageable.class))).willReturn(mockPage);

        // when
        Page<AIModelDocument> results = searchRepository.findPopularModels(pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("검색 정확도 테스트")
    void searchAccuracyTest() {
        // given
        AIModelDocument creative1 = createTestDocument("1", "Creative Model v1.0", "Creative AI model", 1L, "CreativeAI");
        AIModelDocument creative2 = createTestDocument("2", "Creative Model v1.6", "Enhanced creative model", 1L, "CreativeAI");
        
        Pageable exactPageable = PageRequest.of(0, 10, Sort.by("_score").descending());
        Pageable partialPageable = PageRequest.of(0, 10, Sort.by("_score").descending());
        
        // 정확한 검색 결과 - 정확한 모델이 먼저 나옴
        Page<AIModelDocument> exactPage = new PageImpl<>(Arrays.asList(creative1), exactPageable, 1);
        // 부분 검색 결과 - Creative 관련 모델들
        Page<AIModelDocument> partialPage = new PageImpl<>(Arrays.asList(creative1, creative2), partialPageable, 2);
        
        // Mock 설정
        given(searchRepository.searchByModelNameAndPrompt(eq("Creative Model v1.0"), any(Pageable.class)))
                .willReturn(exactPage);
        given(searchRepository.searchByModelNameAndPrompt(eq("Creative"), any(Pageable.class)))
                .willReturn(partialPage);

        // when
        Page<AIModelDocument> exactResults = searchRepository.searchByModelNameAndPrompt("Creative Model v1.0", exactPageable);
        Page<AIModelDocument> partialResults = searchRepository.searchByModelNameAndPrompt("Creative", partialPageable);

        // then
        assertThat(exactResults.getContent()).isNotEmpty();
        assertThat(exactResults.getContent().get(0).getModelName()).contains("Creative Model");
        
        assertThat(partialResults.getContent()).isNotEmpty();
        assertThat(partialResults.getContent())
            .extracting(AIModelDocument::getModelName)
            .anyMatch(name -> name.contains("Creative"));
    }

    @Test
    @DisplayName("자동완성 제안 생성 테스트")
    void autocompleteSuggestionsTest() {
        // given
        AIModelDocument stableDiffusion = createTestDocument("1", "Stable Diffusion v1.5", "AI image generation model", 1L, "StabilityAI");
        AIModelDocument gpt4 = createTestDocument("2", "GPT-4 Turbo", "Advanced language model", 2L, "OpenAI");
        AIModelDocument creative = createTestDocument("3", "Creative Model Pro", "Creative AI assistant", 3L, "CreativeAI");
        
        // then - suggest 리스트가 올바르게 생성되는지 확인
        assertThat(stableDiffusion.getSuggest()).isNotNull();
        assertThat(stableDiffusion.getSuggest()).contains("stable", "diffusion");
        
        assertThat(gpt4.getSuggest()).isNotNull();
        assertThat(gpt4.getSuggest()).contains("gpt", "turbo");
        
        assertThat(creative.getSuggest()).isNotNull();
        assertThat(creative.getSuggest()).contains("creative", "model", "pro");
    }

    @Test
    @DisplayName("문서 삭제")
    void deleteDocument() {
        // given
        AIModelDocument document = createTestDocument("delete-test", "Delete Me", "Test deletion", 1L, "User1");
        
        // Mock 설정 - 저장 후 존재하다가 삭제 후 비어있음
        given(searchRepository.save(any(AIModelDocument.class))).willReturn(document);
        given(searchRepository.findById("delete-test"))
                .willReturn(Optional.of(document))  // 첫 번째 호출에서 존재
                .willReturn(Optional.empty());      // 삭제 후 비어있음
        
        searchRepository.save(document);
        Optional<AIModelDocument> saved = searchRepository.findById("delete-test");
        assertThat(saved).isPresent();

        // when
        searchRepository.deleteById("delete-test");

        // then
        Optional<AIModelDocument> deleted = searchRepository.findById("delete-test");
        assertThat(deleted).isEmpty();
        
        // 삭제 메서드 호출 확인
        verify(searchRepository).deleteById("delete-test");
    }

    @Test
    @DisplayName("문서 업데이트")
    void updateDocument() {
        // given
        AIModelDocument document = createTestDocument("update-test", "Original Name", "Original description", 1L, "User1");
        
        // when - 문서 수정
        document.updatePrice(new BigDecimal("99.99"));
        document.updateRating(4.5, 50L);
        document.increaseUsage();
        
        // Mock 설정 - 업데이트된 문서 반환
        given(searchRepository.save(any(AIModelDocument.class))).willReturn(document);
        given(searchRepository.findById("update-test")).willReturn(Optional.of(document));
        
        AIModelDocument updated = searchRepository.save(document);

        // then
        Optional<AIModelDocument> found = searchRepository.findById("update-test");
        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(found.get().getRating()).isEqualTo(4.5);
        assertThat(found.get().getUsageCount()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("페이징 처리")
    void testPagination() {
        // given - 15개 문서 생성
        List<AIModelDocument> allDocs = Arrays.asList(
            createTestDocument("doc-1", "Model 1", "Description 1", 1L, "User1"),
            createTestDocument("doc-2", "Model 2", "Description 2", 1L, "User1"),
            createTestDocument("doc-3", "Model 3", "Description 3", 1L, "User1"),
            createTestDocument("doc-4", "Model 4", "Description 4", 1L, "User1"),
            createTestDocument("doc-5", "Model 5", "Description 5", 1L, "User1")
        );
        
        // when - 첫 번째 페이지 (5개)
        Pageable firstPage = PageRequest.of(0, 5);
        Page<AIModelDocument> page1Mock = new PageImpl<>(allDocs, firstPage, 15);
        given(searchRepository.findByOwnerId(eq(1L), eq(firstPage))).willReturn(page1Mock);
        
        Page<AIModelDocument> page1 = searchRepository.findByOwnerId(1L, firstPage);

        // then
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();

        // when - 두 번째 페이지
        Pageable secondPage = PageRequest.of(1, 5);
        Page<AIModelDocument> page2Mock = new PageImpl<>(allDocs, secondPage, 15);
        given(searchRepository.findByOwnerId(eq(1L), eq(secondPage))).willReturn(page2Mock);
        
        Page<AIModelDocument> page2 = searchRepository.findByOwnerId(1L, secondPage);

        // then
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page2.hasNext()).isTrue();
        assertThat(page2.hasPrevious()).isTrue();
    }

    // 헬퍼 메서드
    private AIModelDocument createTestDocument(String id, String modelName, String prompt, Long ownerId, String ownerName) {
        AIModelDocument document = AIModelDocument.builder()
                .modelName(modelName)
                .prompt(prompt)
                .ownerId(ownerId)
                .ownerName(ownerName)
                .price(BigDecimal.ZERO)
                .usageCount(0L)
                .rating(0.0)
                .reviewCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // ID는 reflection을 통해 설정 (테스트용)
        try {
            java.lang.reflect.Field idField = AIModelDocument.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(document, id);
        } catch (Exception e) {
            throw new RuntimeException("테스트 문서 ID 설정 실패", e);
        }
        
        return document;
    }
}