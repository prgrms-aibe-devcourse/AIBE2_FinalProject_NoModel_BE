package com.example.nomodel.model.query.controller;

import com.example.nomodel._core.base.BaseUnitTest;
import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.security.CustomUserDetailsService;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.model.command.application.dto.response.ModelUsageCountResponse;
import com.example.nomodel.model.command.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.command.application.dto.response.ModelUsageHistoryResponse;
import com.example.nomodel.model.query.service.ModelUsageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static com.example.nomodel._core.restdocs.RestDocsConfig.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ModelUsageController.class)
@DisplayName("ModelUsageController 단위 테스트")
class ModelUsageControllerTest extends BaseUnitTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ModelUsageService modelUsageService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JWTTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("모델 사용 내역 조회 - 전체 모델, 성공")
    @WithMockUser
    void getModelUsageHistory_AllModels_Success() throws Exception {
        // Given
        Long memberId = 1L;
        ModelUsageHistoryResponse history1 = ModelUsageHistoryResponse.builder()
            .adResultId(1L)
            .modelId(100L)
            .modelName("GPT-4")
            .modelImageUrl("https://example.com/model1.jpg")
            .prompt("테스트 프롬프트 1")
            .createdAt(LocalDateTime.now())
            .build();
        
        ModelUsageHistoryResponse history2 = ModelUsageHistoryResponse.builder()
            .adResultId(2L)
            .modelId(200L)
            .modelName("DALL-E")
            .modelImageUrl("https://example.com/model2.jpg")
            .prompt("테스트 프롬프트 2")
            .createdAt(LocalDateTime.now())
            .build();
        
        ModelUsageHistoryPageResponse pageResponse = ModelUsageHistoryPageResponse.builder()
            .content(Arrays.asList(history1, history2))
            .pageNumber(0)
            .pageSize(20)
            .totalElements(2L)
            .totalPages(1)
            .hasNext(false)
            .hasPrevious(false)
            .build();
        
        given(modelUsageService.getModelUsageHistory(eq(memberId), isNull(), eq(0), eq(20)))
            .willReturn(pageResponse);
        
        // When & Then
        mockMvc.perform(get("/members/me/models/usage")
                .with(user(createCustomUserDetails(memberId)))
                .param("page", "0")
                .param("size", "20"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response.content").isArray())
            .andExpect(jsonPath("$.response.content.length()").value(2))
            .andExpect(jsonPath("$.response.content[0].adResultId").value(1))
            .andExpect(jsonPath("$.response.content[0].modelId").value(100))
            .andExpect(jsonPath("$.response.content[0].modelName").value("GPT-4"))
            .andExpect(jsonPath("$.response.content[0].prompt").value("테스트 프롬프트 1"))
            .andExpect(jsonPath("$.response.content[0].modelImageUrl").value("https://example.com/model1.jpg"))
            .andExpect(jsonPath("$.response.totalElements").value(2))
            .andExpect(jsonPath("$.response.pageNumber").value(0))
            .andExpect(jsonPath("$.response.pageSize").value(20))
            .andDo(document("model-usage-history",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                queryParameters(modelUsageHistoryParams()),
                responseFields(modelUsageHistoryResponse())
            ));
        
        verify(modelUsageService).getModelUsageHistory(memberId, null, 0, 20);
    }

    @Test
    @DisplayName("모델 사용 내역 조회 - 특정 모델, 성공")
    @WithMockUser
    void getModelUsageHistory_SpecificModel_Success() throws Exception {
        // Given
        Long memberId = 1L;
        Long modelId = 100L;
        
        ModelUsageHistoryResponse history = ModelUsageHistoryResponse.builder()
            .adResultId(1L)
            .modelId(modelId)
            .modelName("GPT-4")
            .modelImageUrl("https://example.com/specific-model.jpg")
            .prompt("특정 모델 프롬프트")
            .createdAt(LocalDateTime.now())
            .build();
        
        ModelUsageHistoryPageResponse pageResponse = ModelUsageHistoryPageResponse.builder()
            .content(Collections.singletonList(history))
            .pageNumber(0)
            .pageSize(20)
            .totalElements(1L)
            .totalPages(1)
            .hasNext(false)
            .hasPrevious(false)
            .build();
        
        given(modelUsageService.getModelUsageHistory(eq(memberId), eq(modelId), eq(0), eq(20)))
            .willReturn(pageResponse);
        
        // When & Then
        mockMvc.perform(get("/members/me/models/usage")
                .with(user(createCustomUserDetails(memberId)))
                .param("modelId", modelId.toString())
                .param("page", "0")
                .param("size", "20"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response.content").isArray())
            .andExpect(jsonPath("$.response.content.length()").value(1))
            .andExpect(jsonPath("$.response.content[0].modelId").value(modelId))
            .andExpect(jsonPath("$.response.totalElements").value(1));
        
        verify(modelUsageService).getModelUsageHistory(memberId, modelId, 0, 20);
    }

    @Test
    @DisplayName("모델 사용 내역 조회 - 빈 결과, 성공")
    @WithMockUser
    void getModelUsageHistory_EmptyResult_Success() throws Exception {
        // Given
        Long memberId = 1L;
        
        ModelUsageHistoryPageResponse emptyPageResponse = ModelUsageHistoryPageResponse.builder()
            .content(Collections.emptyList())
            .pageNumber(0)
            .pageSize(20)
            .totalElements(0L)
            .totalPages(0)
            .hasNext(false)
            .hasPrevious(false)
            .build();
        
        given(modelUsageService.getModelUsageHistory(eq(memberId), isNull(), eq(0), eq(20)))
            .willReturn(emptyPageResponse);
        
        // When & Then
        mockMvc.perform(get("/members/me/models/usage")
                .with(user(createCustomUserDetails(memberId))))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response.content").isArray())
            .andExpect(jsonPath("$.response.content").isEmpty())
            .andExpect(jsonPath("$.response.totalElements").value(0));
        
        verify(modelUsageService).getModelUsageHistory(memberId, null, 0, 20);
    }

    @Test
    @DisplayName("모델 사용 내역 조회 - 잘못된 페이지 파라미터")
    @WithMockUser
    void getModelUsageHistory_InvalidPageParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/members/me/models/usage")
                .with(user(createCustomUserDetails(1L)))
                .param("page", "-1")
                .param("size", "0"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.status").value(500));
    }

    @Test
    @DisplayName("모델 사용 내역 조회 - 페이지 크기 초과")
    @WithMockUser
    void getModelUsageHistory_ExceedMaxPageSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/members/me/models/usage")
                .with(user(createCustomUserDetails(1L)))
                .param("size", "101"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.status").value(500));
    }

    @Test
    @DisplayName("모델 사용 횟수 조회 - 성공")
    @WithMockUser
    void getModelUsageCount_Success() throws Exception {
        // Given
        Long memberId = 1L;
        ModelUsageCountResponse countResponse = ModelUsageCountResponse.builder()
            .totalCount(15L)
            .build();
        
        given(modelUsageService.getModelUsageCount(eq(memberId)))
            .willReturn(countResponse);
        
        // When & Then
        mockMvc.perform(get("/members/me/models/usage/count")
                .with(user(createCustomUserDetails(memberId))))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response.totalCount").value(15))
            .andDo(document("model-usage-count",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(modelUsageCountResponse())
            ));
        
        verify(modelUsageService).getModelUsageCount(memberId);
    }

    @Test
    @DisplayName("모델 사용 횟수 조회 - 0건")
    @WithMockUser
    void getModelUsageCount_ZeroCount() throws Exception {
        // Given
        Long memberId = 1L;
        ModelUsageCountResponse countResponse = ModelUsageCountResponse.builder()
            .totalCount(0L)
            .build();
        
        given(modelUsageService.getModelUsageCount(eq(memberId)))
            .willReturn(countResponse);
        
        // When & Then
        mockMvc.perform(get("/members/me/models/usage/count")
                .with(user(createCustomUserDetails(memberId))))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.response.totalCount").value(0));
        
        verify(modelUsageService).getModelUsageCount(memberId);
    }

    @Test
    @DisplayName("인증되지 않은 사용자 요청")
    void getModelUsageHistory_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/members/me/models/usage"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
        
        verifyNoInteractions(modelUsageService);
    }

    private CustomUserDetails createCustomUserDetails(Long memberId) {
        return new CustomUserDetails(
            memberId,
            "test@example.com",
            "encodedPassword",
            Collections.emptyList()
        );
    }
}