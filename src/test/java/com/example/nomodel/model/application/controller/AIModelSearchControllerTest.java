package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.base.BaseUnitTest;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.domain.document.AIModelDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.nomodel._core.config.RestDocsConfiguration.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AIModelSearchController.class)
@DisplayName("AIModelSearchController 단위 테스트")
class AIModelSearchControllerTest extends BaseUnitTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AIModelSearchService searchService;

    @Test
    @DisplayName("통합 검색 성공")
    void searchModels_Success() throws Exception {
        // given
        String keyword = "GPT";
        AIModelDocument document = createMockDocument("1", "GPT-4", "Advanced language model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.search(eq(keyword), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search")
                        .param("keyword", keyword)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.response.content").isArray())
                .andExpect(jsonPath("$.response.content[0].modelName").value("GPT-4"))
                .andDo(document("model-search",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드"),
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(searchSuccessResponse())
                ));

        then(searchService).should().search(eq(keyword), eq(0), eq(10));
    }

    @Test
    @DisplayName("통합 검색 - 기본 파라미터")
    void searchModels_DefaultParameters() throws Exception {
        // given
        String keyword = "BERT";
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of());
        
        given(searchService.search(eq(keyword), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content").isArray());

        then(searchService).should().search(eq(keyword), eq(0), eq(10));
    }

    @Test
    @DisplayName("접근 가능한 모델 검색 성공")
    void searchAccessibleModels_Success() throws Exception {
        // given
        String keyword = "ChatGPT";
        Long userId = 1L;
        AIModelDocument document = createMockDocument("1", "ChatGPT", "Conversational AI");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.searchAccessibleModels(eq(keyword), eq(userId), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/accessible")
                        .param("keyword", keyword)
                        .param("userId", userId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("ChatGPT"));

        then(searchService).should().searchAccessibleModels(eq(keyword), eq(userId), eq(0), eq(10));
    }

    @Test
    @DisplayName("고급 검색 성공")
    void advancedSearch_Success() throws Exception {
        // given
        String keyword = "language model";
        String tag = "NLP";
        AIModelDocument document = createMockDocument("1", "BERT", "Language understanding model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.advancedSearch(eq(keyword), eq(tag), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/advanced")
                        .param("keyword", keyword)
                        .param("tag", tag)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("BERT"));

        then(searchService).should().advancedSearch(eq(keyword), eq(tag), eq(0), eq(10));
    }

    @Test
    @DisplayName("복합 필터 검색 성공")
    void searchWithFilters_Success() throws Exception {
        // given
        String keyword = "image generation";
        String tag = "CV";
        BigDecimal minPrice = new BigDecimal("10");
        BigDecimal maxPrice = new BigDecimal("100");
        
        AIModelDocument document = createMockDocument("1", "DALL-E", "Image generation model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.searchWithFilters(eq(keyword), eq(tag), eq(minPrice), eq(maxPrice), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/filter")
                        .param("keyword", keyword)
                        .param("tag", tag)
                        .param("minPrice", minPrice.toString())
                        .param("maxPrice", maxPrice.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("DALL-E"));

        then(searchService).should().searchWithFilters(eq(keyword), eq(tag), eq(minPrice), eq(maxPrice), eq(0), eq(10));
    }

    @Test
    @DisplayName("태그별 검색 성공")
    void searchByTag_Success() throws Exception {
        // given
        String tag = "NLP";
        AIModelDocument document = createMockDocument("1", "BERT", "NLP model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.searchByTag(eq(tag), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/tag")
                        .param("tag", tag)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("BERT"));

        then(searchService).should().searchByTag(eq(tag), eq(0), eq(10));
    }

    @Test
    @DisplayName("소유자별 검색 성공")
    void searchByOwner_Success() throws Exception {
        // given
        Long ownerId = 1L;
        AIModelDocument document = createMockDocument("1", "MyModel", "Owner's model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.searchByOwner(eq(ownerId), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/owner/{ownerId}", ownerId)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("MyModel"))
                .andDo(document("model-search-by-owner",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("ownerId").description("모델 소유자 ID")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(searchSuccessResponse())
                ));

        then(searchService).should().searchByOwner(eq(ownerId), eq(0), eq(10));
    }

    @Test
    @DisplayName("인기 모델 검색 성공")
    void getPopularModels_Success() throws Exception {
        // given
        AIModelDocument document = createMockDocument("1", "PopularModel", "High usage model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getPopularModels(eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/popular")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("PopularModel"))
                .andDo(document("model-popular",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(searchSuccessResponse())
                ));

        then(searchService).should().getPopularModels(eq(0), eq(10));
    }

    @Test
    @DisplayName("최신 모델 검색 성공")
    void getRecentModels_Success() throws Exception {
        // given
        AIModelDocument document = createMockDocument("1", "NewModel", "Recently created model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getRecentModels(eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/recent")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("NewModel"));

        then(searchService).should().getRecentModels(eq(0), eq(10));
    }

    @Test
    @DisplayName("관리자 추천 모델 검색 성공")
    void getRecommendedModels_Success() throws Exception {
        // given
        AIModelDocument document = createMockDocument("1", "RecommendedModel", "Admin recommended");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getRecommendedModels(eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/recommended")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("RecommendedModel"));

        then(searchService).should().getRecommendedModels(eq(0), eq(10));
    }

    @Test
    @DisplayName("고평점 모델 검색 성공")
    void getHighRatedModels_Success() throws Exception {
        // given
        Double minRating = 4.0;
        AIModelDocument document = createMockDocument("1", "HighRatedModel", "Excellent model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getHighRatedModels(eq(minRating), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/high-rated")
                        .param("minRating", minRating.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("HighRatedModel"));

        then(searchService).should().getHighRatedModels(eq(minRating), eq(0), eq(10));
    }

    @Test
    @DisplayName("고평점 모델 검색 - 기본 최소 평점")
    void getHighRatedModels_DefaultMinRating() throws Exception {
        // given
        AIModelDocument document = createMockDocument("1", "HighRatedModel", "Excellent model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getHighRatedModels(eq(4.0), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/high-rated")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(searchService).should().getHighRatedModels(eq(4.0), eq(0), eq(10));
    }

    @Test
    @DisplayName("무료 모델 검색 성공")
    void getFreeModels_Success() throws Exception {
        // given
        AIModelDocument document = createMockDocument("1", "FreeModel", "Free to use model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getFreeModels(eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/free")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("FreeModel"));

        then(searchService).should().getFreeModels(eq(0), eq(10));
    }

    @Test
    @DisplayName("가격 범위 검색 성공")
    void searchByPriceRange_Success() throws Exception {
        // given
        BigDecimal minPrice = new BigDecimal("10");
        BigDecimal maxPrice = new BigDecimal("50");
        AIModelDocument document = createMockDocument("1", "AffordableModel", "Budget-friendly model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.searchByPriceRange(eq(minPrice), eq(maxPrice), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/price-range")
                        .param("minPrice", minPrice.toString())
                        .param("maxPrice", maxPrice.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("AffordableModel"));

        then(searchService).should().searchByPriceRange(eq(minPrice), eq(maxPrice), eq(0), eq(10));
    }

    @Test
    @DisplayName("자동완성 제안 성공")
    void getModelNameSuggestions_Success() throws Exception {
        // given
        String prefix = "GPT";
        List<String> mockSuggestions = Arrays.asList("GPT-4", "GPT-3.5");
        
        given(searchService.getModelNameSuggestions(eq(prefix)))
                .willReturn(mockSuggestions);

        // when & then
        mockMvc.perform(get("/models/search/suggestions")
                        .param("prefix", prefix))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response[0]").value("GPT-4"))
                .andExpect(jsonPath("$.response[1]").value("GPT-3.5"));

        then(searchService).should().getModelNameSuggestions(eq(prefix));
    }

    @Test
    @DisplayName("유사 모델 검색 성공")
    void getSimilarModels_Success() throws Exception {
        // given
        String modelId = "model-123";
        AIModelDocument document = createMockDocument("1", "SimilarModel", "Similar to target model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.getSimilarModels(eq(modelId), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/similar/{modelId}", modelId)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("SimilarModel"));

        then(searchService).should().getSimilarModels(eq(modelId), eq(0), eq(10));
    }

    @Test
    @DisplayName("모델 상세 정보 조회 성공")
    void getModelDetail_Success() throws Exception {
        // given
        String documentId = "doc-123";
        AIModelDocument document = createMockDocument(documentId, "TestModel", "Test model");
        
        given(searchService.findById(eq(documentId)))
                .willReturn(Optional.of(document));

        // when & then
        mockMvc.perform(get("/models/search/{documentId}", documentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.id").value(documentId))
                .andExpect(jsonPath("$.response.modelName").value("TestModel"));

        then(searchService).should().findById(eq(documentId));
    }

    @Test
    @DisplayName("모델 상세 정보 조회 실패 - 문서 없음")
    void getModelDetail_NotFound() throws Exception {
        // given
        String documentId = "nonexistent-doc";
        
        given(searchService.findById(eq(documentId)))
                .willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/models/search/{documentId}", documentId))
                .andDo(print())
                .andExpect(status().isNotFound());

        then(searchService).should().findById(eq(documentId));
    }

    @Test
    @DisplayName("하이라이트 검색 성공")
    void searchWithHighlight_Success() throws Exception {
        // given
        String keyword = "machine learning";
        AIModelDocument document = createMockDocument("1", "ML Model", "Machine learning model");
        Page<AIModelDocument> mockPage = new PageImpl<>(List.of(document));
        
        given(searchService.searchWithHighlight(eq(keyword), eq(0), eq(10)))
                .willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/models/search/highlight")
                        .param("keyword", keyword)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content[0].modelName").value("ML Model"));

        then(searchService).should().searchWithHighlight(eq(keyword), eq(0), eq(10));
    }

    @Test
    @DisplayName("모델 사용량 증가 성공")
    void increaseUsage_Success() throws Exception {
        // given
        String documentId = "doc-123";

        // when & then
        mockMvc.perform(post("/models/search/{documentId}/usage", documentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").doesNotExist());

        then(searchService).should().increaseUsage(eq(documentId));
    }

    // 헬퍼 메서드
    private AIModelDocument createMockDocument(String id, String modelName, String prompt) {
        AIModelDocument document = mock(AIModelDocument.class);
        given(document.getId()).willReturn(id);
        given(document.getModelName()).willReturn(modelName);
        given(document.getPrompt()).willReturn(prompt);
        return document;
    }
}