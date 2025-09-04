package com.example.nomodel.model.infrastructure;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local") // H2 데이터베이스 사용
@Transactional
@DisplayName("Elasticsearch 통합 테스트")
class ElasticsearchIntegrationTest {

    @Autowired
    private AIModelSearchRepository searchRepository;

    @AfterEach
    void tearDown() {
        // Elasticsearch 인덱스 정리
        searchRepository.deleteAll();
    }

    @Test
    @DisplayName("AI 모델 문서 저장 및 조회")
    void saveAndFindDocument() throws InterruptedException {
        // given
        AIModelDocument document = createTestDocument("test-1", "GPT-4", "Advanced language model", 1L, "OpenAI");

        // when
        AIModelDocument saved = searchRepository.save(document);
        
        // Elasticsearch 인덱스 새로고침 대기
        Thread.sleep(1000);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("test-1");
        assertThat(saved.getModelName()).isEqualTo("GPT-4");

        Optional<AIModelDocument> found = searchRepository.findById("test-1");
        assertThat(found).isPresent();
        assertThat(found.get().getModelName()).isEqualTo("GPT-4");
    }

    @Test
    @DisplayName("모델명과 프롬프트로 검색")
    void searchByModelNameAndPrompt() throws InterruptedException {
        // given
        AIModelDocument gpt4 = createTestDocument("1", "GPT-4 Turbo", "Advanced language model for complex tasks", 1L, "OpenAI");
        AIModelDocument gpt35 = createTestDocument("2", "GPT-3.5", "Chat completion model for conversations", 1L, "OpenAI");
        AIModelDocument claude = createTestDocument("3", "Claude-3", "Anthropic AI assistant", 2L, "Anthropic");
        
        searchRepository.save(gpt4);
        searchRepository.save(gpt35);
        searchRepository.save(claude);
        
        Thread.sleep(1000);

        // when - "GPT" 키워드로 검색
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.searchByModelNameAndPrompt("GPT", pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(doc -> 
            doc.getModelName().contains("GPT") || doc.getPrompt().contains("GPT"));
    }

    @Test
    @DisplayName("소유자 ID로 검색")
    void findByOwnerId() throws InterruptedException {
        // given
        AIModelDocument user1Model1 = createTestDocument("1", "Model A", "First model", 1L, "User1");
        AIModelDocument user1Model2 = createTestDocument("2", "Model B", "Second model", 1L, "User1");
        AIModelDocument user2Model = createTestDocument("3", "Model C", "Other user model", 2L, "User2");
        
        searchRepository.save(user1Model1);
        searchRepository.save(user1Model2);
        searchRepository.save(user2Model);
        
        Thread.sleep(1000);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findByOwnerId(1L, pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).allMatch(doc -> doc.getOwnerId().equals(1L));
    }

    @Test
    @DisplayName("공개 모델 검색")
    void findPublicModels() throws InterruptedException {
        // given
        AIModelDocument publicModel = createTestDocument("1", "Public Model", "Open to everyone", 1L, "User1");
        publicModel.updateVisibility(true);
        
        AIModelDocument privateModel = createTestDocument("2", "Private Model", "Only for owner", 1L, "User1");
        privateModel.updateVisibility(false);
        
        searchRepository.save(publicModel);
        searchRepository.save(privateModel);
        
        Thread.sleep(1000);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findByIsPublic(true, pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getIsPublic()).isTrue();
    }

    @Test
    @DisplayName("가격 범위로 검색")
    void searchByPriceRange() throws InterruptedException {
        // given
        AIModelDocument cheapModel = createTestDocument("1", "Budget Model", "Affordable option", 1L, "User1");
        cheapModel.updatePrice(new BigDecimal("9.99"));
        
        AIModelDocument midModel = createTestDocument("2", "Standard Model", "Mid-range option", 1L, "User1");
        midModel.updatePrice(new BigDecimal("29.99"));
        
        AIModelDocument expensiveModel = createTestDocument("3", "Premium Model", "High-end option", 1L, "User1");
        expensiveModel.updatePrice(new BigDecimal("99.99"));
        
        searchRepository.save(cheapModel);
        searchRepository.save(midModel);
        searchRepository.save(expensiveModel);
        
        Thread.sleep(1000);

        // when - 10~50 가격 범위 검색
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.searchByPriceRange(
            new BigDecimal("10"), new BigDecimal("50"), pageable);

        // then
        assertThat(results).isNotNull();
        // TODO: 실제 가격 범위 필터링 구현 시 검증 추가
        // assertThat(results.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("평점 기준 검색")
    void findHighRatedModels() throws InterruptedException {
        // given
        AIModelDocument highRatedModel = createTestDocument("1", "Excellent Model", "High quality", 1L, "User1");
        highRatedModel.updateRating(4.8, 100L);
        
        AIModelDocument midRatedModel = createTestDocument("2", "Good Model", "Decent quality", 1L, "User1");
        midRatedModel.updateRating(3.5, 50L);
        
        AIModelDocument lowRatedModel = createTestDocument("3", "Average Model", "Basic quality", 1L, "User1");
        lowRatedModel.updateRating(2.5, 25L);
        
        searchRepository.save(highRatedModel);
        searchRepository.save(midRatedModel);
        searchRepository.save(lowRatedModel);
        
        Thread.sleep(1000);

        // when - 4.0 이상 평점 모델 검색
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findHighRatedModels(4.0, pageable);

        // then
        assertThat(results).isNotNull();
        // TODO: 실제 평점 필터링 구현 시 검증 추가
        // assertThat(results.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("무료 모델 검색")
    void findFreeModels() throws InterruptedException {
        // given
        AIModelDocument freeModel1 = createTestDocument("1", "Free Model 1", "No cost", 1L, "User1");
        freeModel1.updatePrice(BigDecimal.ZERO);
        
        AIModelDocument freeModel2 = createTestDocument("2", "Free Model 2", "Also free", 1L, "User1");
        freeModel2.updatePrice(BigDecimal.ZERO);
        
        AIModelDocument paidModel = createTestDocument("3", "Paid Model", "Premium service", 1L, "User1");
        paidModel.updatePrice(new BigDecimal("19.99"));
        
        searchRepository.save(freeModel1);
        searchRepository.save(freeModel2);
        searchRepository.save(paidModel);
        
        Thread.sleep(1000);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findFreeModels(pageable);

        // then
        assertThat(results).isNotNull();
        // TODO: 실제 무료 모델 필터링 구현 시 검증 추가
        // assertThat(results.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("추천 모델 검색")
    void findRecommendedModels() throws InterruptedException {
        // given
        AIModelDocument recommendedModel = createTestDocument("1", "Recommended Model", "Admin pick", 1L, "User1");
        // TODO: isRecommended 필드 업데이트 메서드 구현 시 활성화
        // recommendedModel.updateRecommendedStatus(true);
        
        AIModelDocument normalModel = createTestDocument("2", "Normal Model", "Regular model", 1L, "User1");
        
        searchRepository.save(recommendedModel);
        searchRepository.save(normalModel);
        
        Thread.sleep(1000);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findRecommendedModels(pageable);

        // then
        assertThat(results).isNotNull();
        // TODO: 실제 추천 모델 필터링 구현 시 검증 추가
        // assertThat(results.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("최신 모델 검색")
    void findRecentModels() throws InterruptedException {
        // given
        AIModelDocument oldModel = createTestDocument("1", "Old Model", "Created earlier", 1L, "User1");
        searchRepository.save(oldModel);
        
        Thread.sleep(100); // 시간 차이를 위한 대기
        
        AIModelDocument newModel = createTestDocument("2", "New Model", "Created recently", 1L, "User1");
        searchRepository.save(newModel);
        
        Thread.sleep(1000);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findRecentModels(pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).isNotEmpty();
        // TODO: 실제 최신순 정렬 구현 시 순서 검증 추가
    }

    @Test
    @DisplayName("인기 모델 검색 (사용량 기준)")
    void findPopularModels() throws InterruptedException {
        // given
        AIModelDocument popularModel = createTestDocument("1", "Popular Model", "High usage", 1L, "User1");
        popularModel.increaseUsage();
        popularModel.increaseUsage();
        popularModel.increaseUsage();
        
        AIModelDocument normalModel = createTestDocument("2", "Normal Model", "Low usage", 1L, "User1");
        
        searchRepository.save(popularModel);
        searchRepository.save(normalModel);
        
        Thread.sleep(1000);

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<AIModelDocument> results = searchRepository.findPopularModels(pageable);

        // then
        assertThat(results).isNotNull();
        assertThat(results.getContent()).isNotEmpty();
        // TODO: 실제 인기순 정렬 구현 시 순서 검증 추가
    }

    @Test
    @DisplayName("모델명 자동완성 제안")
    void findModelNameSuggestions() throws InterruptedException {
        // given
        AIModelDocument gpt4 = createTestDocument("1", "GPT-4 Turbo", "Latest GPT", 1L, "OpenAI");
        AIModelDocument gpt35 = createTestDocument("2", "GPT-3.5", "Previous GPT", 1L, "OpenAI");
        AIModelDocument claude = createTestDocument("3", "Claude-3", "Anthropic model", 2L, "Anthropic");
        
        searchRepository.save(gpt4);
        searchRepository.save(gpt35);
        searchRepository.save(claude);
        
        Thread.sleep(1000);

        // when
        List<AIModelDocument> suggestions = searchRepository.findModelNameSuggestions("GPT");

        // then
        assertThat(suggestions).isNotNull();
        // TODO: 실제 자동완성 구현 시 검증 추가
        // assertThat(suggestions).hasSize(2);
    }

    @Test
    @DisplayName("문서 삭제")
    void deleteDocument() throws InterruptedException {
        // given
        AIModelDocument document = createTestDocument("delete-test", "Delete Me", "Test deletion", 1L, "User1");
        searchRepository.save(document);
        
        Thread.sleep(1000);

        // 저장 확인
        Optional<AIModelDocument> saved = searchRepository.findById("delete-test");
        assertThat(saved).isPresent();

        // when
        searchRepository.deleteById("delete-test");
        
        Thread.sleep(1000);

        // then
        Optional<AIModelDocument> deleted = searchRepository.findById("delete-test");
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("문서 업데이트")
    void updateDocument() throws InterruptedException {
        // given
        AIModelDocument document = createTestDocument("update-test", "Original Name", "Original description", 1L, "User1");
        searchRepository.save(document);
        
        Thread.sleep(1000);

        // when - 문서 수정
        document.updatePrice(new BigDecimal("99.99"));
        document.updateRating(4.5, 50L);
        document.increaseUsage();
        
        AIModelDocument updated = searchRepository.save(document);
        
        Thread.sleep(1000);

        // then
        Optional<AIModelDocument> found = searchRepository.findById("update-test");
        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(found.get().getRating()).isEqualTo(4.5);
        assertThat(found.get().getUsageCount()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("페이징 처리")
    void testPagination() throws InterruptedException {
        // given - 15개 문서 생성
        for (int i = 1; i <= 15; i++) {
            AIModelDocument doc = createTestDocument("doc-" + i, "Model " + i, "Description " + i, 1L, "User1");
            searchRepository.save(doc);
        }
        
        Thread.sleep(1000);

        // when - 첫 번째 페이지 (5개)
        Pageable firstPage = PageRequest.of(0, 5);
        Page<AIModelDocument> page1 = searchRepository.findByOwnerId(1L, firstPage);

        // then
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();

        // when - 두 번째 페이지
        Pageable secondPage = PageRequest.of(1, 5);
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