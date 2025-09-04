package com.example.nomodel.model.application.controller;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local") // H2 데이터베이스 사용
@Transactional
@DisplayName("AIModelSearchController 통합 테스트")
class AIModelSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIModelSearchRepository searchRepository;

    @AfterEach
    void tearDown() {
        // Elasticsearch 인덱스 정리
        searchRepository.deleteAll();
    }

    @Test
    @DisplayName("통합 검색 통합 테스트")
    void searchModels_IntegrationTest() throws Exception {
        // given - 테스트 데이터 생성
        AIModelDocument document1 = createTestDocument("1", "GPT-4 Turbo", "Advanced language model by OpenAI", 1L, "OpenAI");
        AIModelDocument document2 = createTestDocument("2", "GPT-3.5", "Chat completion model", 1L, "OpenAI");
        AIModelDocument document3 = createTestDocument("3", "Claude-3", "Anthropic's AI assistant", 2L, "Anthropic");
        
        searchRepository.save(document1);
        searchRepository.save(document2);
        searchRepository.save(document3);
        
        // Elasticsearch 인덱스 새로고침 대기
        Thread.sleep(1000);

        // when & then - GPT 키워드로 검색
        mockMvc.perform(get("/models/search")
                        .param("keyword", "GPT")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.response.content").isArray())
                .andExpect(jsonPath("$.response.totalElements").value(2))
                .andExpect(jsonPath("$.response.content[0].modelName").value(containsString("GPT")));
    }

    @Test
    @DisplayName("접근 가능한 모델 검색 통합 테스트")
    void searchAccessibleModels_IntegrationTest() throws Exception {
        // given - 공개/비공개 모델 데이터 생성
        AIModelDocument publicModel = createTestDocument("1", "Public GPT", "Public model", 1L, "User1");
        publicModel.updateVisibility(true); // 공개
        
        AIModelDocument privateModel = createTestDocument("2", "Private GPT", "Private model", 1L, "User1");
        privateModel.updateVisibility(false); // 비공개
        
        AIModelDocument otherUserModel = createTestDocument("3", "Other Model", "Other user's model", 2L, "User2");
        otherUserModel.updateVisibility(false); // 다른 사용자의 비공개 모델
        
        searchRepository.save(publicModel);
        searchRepository.save(privateModel);
        searchRepository.save(otherUserModel);
        
        Thread.sleep(1000);

        // when & then - 사용자 1이 접근 가능한 모델 검색 (본인 모델 + 공개 모델)
        mockMvc.perform(get("/models/search/accessible")
                        .param("userId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.totalElements").value(2)); // 본인의 2개 모델만
    }

    @Test
    @DisplayName("태그별 검색 통합 테스트")
    void searchByTag_IntegrationTest() throws Exception {
        // given - 태그가 있는 모델 데이터 생성
        AIModelDocument nlpModel = createTestDocument("1", "BERT", "Language understanding model", 1L, "Google");
        // TODO: 태그 기능이 구현되면 태그 설정 추가
        
        AIModelDocument cvModel = createTestDocument("2", "ResNet", "Image classification model", 1L, "Facebook");
        // TODO: 태그 기능이 구현되면 태그 설정 추가
        
        searchRepository.save(nlpModel);
        searchRepository.save(cvModel);
        
        Thread.sleep(1000);

        // when & then - NLP 태그로 검색 (현재는 빈 결과 반환 예상)
        mockMvc.perform(get("/models/search/tag")
                        .param("tag", "NLP")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
                // TODO: 태그 기능 구현 시 실제 결과 검증 추가
    }

    @Test
    @DisplayName("소유자별 검색 통합 테스트")
    void searchByOwner_IntegrationTest() throws Exception {
        // given - 다양한 소유자의 모델 데이터 생성
        AIModelDocument user1Model1 = createTestDocument("1", "User1 Model A", "First model", 1L, "User1");
        AIModelDocument user1Model2 = createTestDocument("2", "User1 Model B", "Second model", 1L, "User1");
        AIModelDocument user2Model = createTestDocument("3", "User2 Model", "Other user model", 2L, "User2");
        
        searchRepository.save(user1Model1);
        searchRepository.save(user1Model2);
        searchRepository.save(user2Model);
        
        Thread.sleep(1000);

        // when & then - 사용자 1의 모델만 검색
        mockMvc.perform(get("/models/search/owner/{ownerId}", 1L)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.totalElements").value(2))
                .andExpect(jsonPath("$.response.content[0].ownerName").value("User1"));
    }

    @Test
    @DisplayName("인기 모델 검색 통합 테스트")
    void getPopularModels_IntegrationTest() throws Exception {
        // given - 사용량이 다른 모델들 생성
        AIModelDocument popularModel = createTestDocument("1", "Popular Model", "High usage model", 1L, "User1");
        popularModel.increaseUsage(); // 사용량 증가
        popularModel.increaseUsage();
        popularModel.increaseUsage();
        
        AIModelDocument normalModel = createTestDocument("2", "Normal Model", "Low usage model", 1L, "User1");
        
        searchRepository.save(popularModel);
        searchRepository.save(normalModel);
        
        Thread.sleep(1000);

        // when & then
        mockMvc.perform(get("/models/search/popular")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists());
    }

    @Test
    @DisplayName("최신 모델 검색 통합 테스트")
    void getRecentModels_IntegrationTest() throws Exception {
        // given - 생성 시간이 다른 모델들 생성
        AIModelDocument oldModel = createTestDocument("1", "Old Model", "Created earlier", 1L, "User1");
        
        AIModelDocument newModel = createTestDocument("2", "New Model", "Created recently", 1L, "User1");
        
        searchRepository.save(oldModel);
        Thread.sleep(100); // 시간 차이를 위한 대기
        searchRepository.save(newModel);
        
        Thread.sleep(1000);

        // when & then
        mockMvc.perform(get("/models/search/recent")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists());
    }

    @Test
    @DisplayName("고평점 모델 검색 통합 테스트")
    void getHighRatedModels_IntegrationTest() throws Exception {
        // given - 평점이 다른 모델들 생성
        AIModelDocument highRatedModel = createTestDocument("1", "Excellent Model", "High rating model", 1L, "User1");
        highRatedModel.updateRating(4.8, 100L);
        
        AIModelDocument lowRatedModel = createTestDocument("2", "Average Model", "Low rating model", 1L, "User1");
        lowRatedModel.updateRating(3.2, 50L);
        
        searchRepository.save(highRatedModel);
        searchRepository.save(lowRatedModel);
        
        Thread.sleep(1000);

        // when & then - 4.0 이상 평점 모델 검색
        mockMvc.perform(get("/models/search/high-rated")
                        .param("minRating", "4.0")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.totalElements").value(1));
    }

    @Test
    @DisplayName("무료 모델 검색 통합 테스트")
    void getFreeModels_IntegrationTest() throws Exception {
        // given - 무료/유료 모델 생성
        AIModelDocument freeModel = createTestDocument("1", "Free Model", "Free to use", 1L, "User1");
        freeModel.updatePrice(BigDecimal.ZERO);
        
        AIModelDocument paidModel = createTestDocument("2", "Paid Model", "Premium model", 1L, "User1");
        paidModel.updatePrice(new BigDecimal("29.99"));
        
        searchRepository.save(freeModel);
        searchRepository.save(paidModel);
        
        Thread.sleep(1000);

        // when & then
        mockMvc.perform(get("/models/search/free")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
                // TODO: 무료 모델 필터링 구현 시 실제 검증 추가
    }

    @Test
    @DisplayName("가격 범위 검색 통합 테스트")
    void searchByPriceRange_IntegrationTest() throws Exception {
        // given - 다양한 가격의 모델들 생성
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

        // when & then - 10~50 가격 범위 검색
        mockMvc.perform(get("/models/search/price-range")
                        .param("minPrice", "10")
                        .param("maxPrice", "50")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
                // TODO: 가격 범위 필터링 구현 시 실제 검증 추가
    }

    @Test
    @DisplayName("자동완성 제안 통합 테스트")
    void getModelNameSuggestions_IntegrationTest() throws Exception {
        // given - 다양한 모델명 생성
        AIModelDocument gpt4 = createTestDocument("1", "GPT-4 Turbo", "Latest GPT model", 1L, "OpenAI");
        AIModelDocument gpt35 = createTestDocument("2", "GPT-3.5", "Previous version", 1L, "OpenAI");
        AIModelDocument claude = createTestDocument("3", "Claude-3", "Anthropic model", 2L, "Anthropic");
        
        searchRepository.save(gpt4);
        searchRepository.save(gpt35);
        searchRepository.save(claude);
        
        Thread.sleep(1000);

        // when & then - "GPT" 접두사로 자동완성
        mockMvc.perform(get("/models/search/suggestions")
                        .param("prefix", "GPT"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").isArray());
                // TODO: 자동완성 구현 시 실제 검증 추가
    }

    @Test
    @DisplayName("모델 사용량 증가 통합 테스트")
    void increaseUsage_IntegrationTest() throws Exception {
        // given - 테스트 모델 생성
        AIModelDocument model = createTestDocument("test-doc-1", "Test Model", "Usage test model", 1L, "User1");
        AIModelDocument savedModel = searchRepository.save(model);
        
        Thread.sleep(1000);

        Long initialUsage = savedModel.getUsageCount();

        // when - 사용량 증가 요청
        mockMvc.perform(post("/models/search/{documentId}/usage", savedModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Thread.sleep(1000);

        // then - 사용량이 증가했는지 확인
        AIModelDocument updatedModel = searchRepository.findById(savedModel.getId()).orElse(null);
        assertThat(updatedModel).isNotNull();
        assertThat(updatedModel.getUsageCount()).isGreaterThan(initialUsage);
    }

    @Test
    @DisplayName("모델 상세 정보 조회 통합 테스트")
    void getModelDetail_IntegrationTest() throws Exception {
        // given - 테스트 모델 생성
        AIModelDocument model = createTestDocument("test-doc-2", "Detailed Model", "Model for detail test", 1L, "User1");
        AIModelDocument savedModel = searchRepository.save(model);
        
        Thread.sleep(1000);

        // when & then
        mockMvc.perform(get("/models/search/{documentId}", savedModel.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.id").value(savedModel.getId()))
                .andExpect(jsonPath("$.response.modelName").value("Detailed Model"));
    }

    @Test
    @DisplayName("존재하지 않는 모델 조회 통합 테스트")
    void getModelDetail_NotFound_IntegrationTest() throws Exception {
        // given - 존재하지 않는 문서 ID
        String nonExistentId = "non-existent-id";

        // when & then
        mockMvc.perform(get("/models/search/{documentId}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound());
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