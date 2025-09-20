package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.base.BaseIntegrationTest;
import com.example.nomodel.model.domain.model.document.AIModelDocument;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel._core.security.WithMockTestUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import java.util.Arrays;
import java.util.Collections;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockTestUser
@DisplayName("AIModelSearchController 통합 테스트")
class AIModelSearchControllerIntegrationTest extends BaseIntegrationTest {

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
        given(searchService.search(eq("GPT"), any(), eq(0), eq(10))).willReturn(mockPage);

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