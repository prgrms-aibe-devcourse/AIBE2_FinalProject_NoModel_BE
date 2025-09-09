package com.example.nomodel.model.application.controller;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel._core.security.WithMockTestUser;
import com.example.nomodel._core.config.TestOAuth2Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // 테스트 프로필 사용
@Transactional
@WithMockTestUser
@Import(TestOAuth2Config.class)
@DisplayName("AIModelSearchController 통합 테스트")
class AIModelSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AIModelSearchService searchService;

    @AfterEach
    void tearDown() {
        // Mock 객체 초기화
        reset(searchService);
    }

    @Test
    @DisplayName("통합 검색 통합 테스트")
    void searchModels_IntegrationTest() throws Exception {
        // given - 테스트 데이터 생성
        AIModelDocument document1 = createTestDocument("1", "GPT-4 Turbo", "Advanced language model by OpenAI", 1L, "OpenAI");
        AIModelDocument document2 = createTestDocument("2", "GPT-3.5", "Chat completion model", 1L, "OpenAI");
        
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(document1, document2), PageRequest.of(0, 10), 2);
        
        // Mock 서비스 메서드 호출
        given(searchService.search(eq("GPT"), eq(0), eq(10))).willReturn(mockPage);

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
        AIModelDocument privateModel = createTestDocument("2", "Private GPT", "Private model", 1L, "User1");
        
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(publicModel, privateModel), PageRequest.of(0, 10), 2);
        
        // Mock 서비스 메서드 호출 - searchAccessibleModels 메서드 모킹 (keyword는 null일 수 있음)
        given(searchService.searchAccessibleModels(any(), eq(1L), eq(0), eq(10))).willReturn(mockPage);

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
        // given - Mock service method
        given(searchService.searchByTag(eq("NLP"), eq(0), eq(10))).willReturn(Page.empty());

        // when & then - NLP 태그로 검색
        mockMvc.perform(get("/models/search/tag")
                        .param("tag", "NLP")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("소유자별 검색 통합 테스트")
    void searchByOwner_IntegrationTest() throws Exception {
        // given - Mock service method
        AIModelDocument user1Model1 = createTestDocument("1", "User1 Model A", "First model", 1L, "User1");
        AIModelDocument user1Model2 = createTestDocument("2", "User1 Model B", "Second model", 1L, "User1");
        
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(user1Model1, user1Model2), PageRequest.of(0, 10), 2);
        given(searchService.searchByOwner(eq(1L), eq(0), eq(10))).willReturn(mockPage);

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
        // given - Mock service method
        given(searchService.getPopularModels(eq(0), eq(10))).willReturn(Page.empty());

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
        // given - Mock service method
        given(searchService.getRecentModels(eq(0), eq(10))).willReturn(Page.empty());

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
        // given - Mock service method with high rated model
        AIModelDocument highRatedModel = createTestDocument("1", "Excellent Model", "High rating model", 1L, "User1");
        highRatedModel.updateRating(4.8, 100L);
        
        Page<AIModelDocument> mockPage = new PageImpl<>(Arrays.asList(highRatedModel), PageRequest.of(0, 10), 1);
        given(searchService.getHighRatedModels(eq(4.0), eq(0), eq(10))).willReturn(mockPage);

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
        // given - Mock service method
        given(searchService.getFreeModels(eq(0), eq(10))).willReturn(Page.empty());

        // when & then
        mockMvc.perform(get("/models/search/free")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("가격 범위 검색 통합 테스트")
    void searchByPriceRange_IntegrationTest() throws Exception {
        // given - Mock service method
        given(searchService.searchByPriceRange(eq(new BigDecimal("10")), eq(new BigDecimal("50")), eq(0), eq(10))).willReturn(Page.empty());

        // when & then - 10~50 가격 범위 검색
        mockMvc.perform(get("/models/search/price-range")
                        .param("minPrice", "10")
                        .param("maxPrice", "50")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("자동완성 제안 통합 테스트")
    void getModelNameSuggestions_IntegrationTest() throws Exception {
        // given - Mock service method
        given(searchService.getModelNameSuggestions(eq("GPT"))).willReturn(Collections.emptyList());

        // when & then - "GPT" 접두사로 자동완성
        mockMvc.perform(get("/models/search/suggestions")
                        .param("prefix", "GPT"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").isArray());
    }

    @Test
    @DisplayName("모델 사용량 증가 통합 테스트")
    void increaseUsage_IntegrationTest() throws Exception {
        // given - Mock service method
        String documentId = "test-doc-1";
        doNothing().when(searchService).increaseUsage(eq(documentId));

        // when - 사용량 증가 요청
        mockMvc.perform(post("/models/search/{documentId}/usage", documentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("모델 상세 정보 조회 통합 테스트")
    void getModelDetail_IntegrationTest() throws Exception {
        // given - Mock service method
        String documentId = "test-doc-2";
        AIModelDocument model = createTestDocument(documentId, "Detailed Model", "Model for detail test", 1L, "User1");
        given(searchService.findById(eq(documentId))).willReturn(java.util.Optional.of(model));

        // when & then
        mockMvc.perform(get("/models/search/{documentId}", documentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.id").value(documentId))
                .andExpect(jsonPath("$.response.modelName").value("Detailed Model"));
    }

    @Test
    @DisplayName("존재하지 않는 모델 조회 통합 테스트")
    void getModelDetail_NotFound_IntegrationTest() throws Exception {
        // given - Mock service method to return empty optional
        String nonExistentId = "non-existent-id";
        given(searchService.findById(eq(nonExistentId))).willReturn(Optional.empty());

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