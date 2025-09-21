package com.example.nomodel.model.query.service;

import com.example.nomodel.model.command.application.dto.ModelUsageProjection;
import com.example.nomodel.model.command.application.dto.response.ModelUsageCountResponse;
import com.example.nomodel.model.command.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.command.application.dto.response.ModelUsageHistoryResponse;
import com.example.nomodel.model.command.domain.repository.ModelUsageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModelUsageService 단위 테스트")
class ModelUsageServiceTest {

    @Mock
    private ModelUsageRepository modelUsageRepository;
    
    @InjectMocks
    private ModelUsageService modelUsageService;

    @Test
    @DisplayName("전체 모델 사용 내역 조회 - 성공")
    void getModelUsageHistory_AllModels_Success() {
        // Given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        ModelUsageProjection projection1 = createModelUsageProjection(
            1L, 100L, "GPT-4", "https://example.com/model1.jpg", "테스트 프롬프트 1");
        ModelUsageProjection projection2 = createModelUsageProjection(
            2L, 200L, "DALL-E", "https://example.com/model2.jpg", "테스트 프롬프트 2");
        List<ModelUsageProjection> projections = Arrays.asList(projection1, projection2);
        Page<ModelUsageProjection> projectionPage = new PageImpl<>(projections, pageable, 2);
        
        given(modelUsageRepository.findModelUsageByMemberId(eq(memberId), eq(pageable)))
            .willReturn(projectionPage);
        
        // When
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(memberId, null, page, size);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        
        ModelUsageHistoryResponse firstItem = response.content().get(0);
        assertThat(firstItem.adResultId()).isEqualTo(1L);
        assertThat(firstItem.modelId()).isEqualTo(100L);
        assertThat(firstItem.modelName()).isEqualTo("GPT-4");
        assertThat(firstItem.modelImageUrl()).isEqualTo("https://example.com/model1.jpg");
        assertThat(firstItem.prompt()).isEqualTo("테스트 프롬프트 1");
        
        verify(modelUsageRepository).findModelUsageByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("특정 모델 사용 내역 조회 - 성공")
    void getModelUsageHistory_SpecificModel_Success() {
        // Given
        Long memberId = 1L;
        Long modelId = 100L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        ModelUsageProjection projection = createModelUsageProjection(
            1L, modelId, "GPT-4", "https://example.com/specific-model.jpg", "특정 모델 프롬프트");
        List<ModelUsageProjection> projections = Collections.singletonList(projection);
        Page<ModelUsageProjection> projectionPage = new PageImpl<>(projections, pageable, 1);
        
        given(modelUsageRepository.findModelUsageByMemberIdAndModelId(eq(memberId), eq(modelId), eq(pageable)))
            .willReturn(projectionPage);
        
        // When
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(memberId, modelId, page, size);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        
        ModelUsageHistoryResponse item = response.content().get(0);
        assertThat(item.adResultId()).isEqualTo(1L);
        assertThat(item.modelId()).isEqualTo(modelId);
        assertThat(item.modelName()).isEqualTo("GPT-4");
        assertThat(item.modelImageUrl()).isEqualTo("https://example.com/specific-model.jpg");
        
        verify(modelUsageRepository).findModelUsageByMemberIdAndModelId(memberId, modelId, pageable);
    }

    @Test
    @DisplayName("빈 사용 내역 조회 - 성공")
    void getModelUsageHistory_EmptyResults_Success() {
        // Given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        Page<ModelUsageProjection> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        given(modelUsageRepository.findModelUsageByMemberId(eq(memberId), eq(pageable)))
            .willReturn(emptyPage);
        
        // When
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(memberId, null, page, size);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        
        verify(modelUsageRepository).findModelUsageByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("모델 이미지가 없는 사용 내역 조회 - 성공")
    void getModelUsageHistory_NoModelImage_Success() {
        // Given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        ModelUsageProjection projection = createModelUsageProjection(
            1L, 100L, "GPT-4", null, "이미지 없는 프롬프트");
        List<ModelUsageProjection> projections = Collections.singletonList(projection);
        Page<ModelUsageProjection> projectionPage = new PageImpl<>(projections, pageable, 1);
        
        given(modelUsageRepository.findModelUsageByMemberId(eq(memberId), eq(pageable)))
            .willReturn(projectionPage);
        
        // When
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(memberId, null, page, size);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        
        ModelUsageHistoryResponse item = response.content().get(0);
        assertThat(item.adResultId()).isEqualTo(1L);
        assertThat(item.modelImageUrl()).isNull();
        
        verify(modelUsageRepository).findModelUsageByMemberId(memberId, pageable);
    }

    @Test
    @DisplayName("모델 사용 횟수 조회 - 성공")
    void getModelUsageCount_Success() {
        // Given
        Long memberId = 1L;
        long expectedCount = 15L;
        
        given(modelUsageRepository.countModelUsageByMemberId(memberId))
            .willReturn(expectedCount);
        
        // When
        ModelUsageCountResponse response = modelUsageService.getModelUsageCount(memberId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalCount()).isEqualTo(expectedCount);
        
        verify(modelUsageRepository).countModelUsageByMemberId(memberId);
    }

    @Test
    @DisplayName("모델 사용 횟수 조회 - 0건")
    void getModelUsageCount_ZeroCount() {
        // Given
        Long memberId = 1L;
        long expectedCount = 0L;
        
        given(modelUsageRepository.countModelUsageByMemberId(memberId))
            .willReturn(expectedCount);
        
        // When
        ModelUsageCountResponse response = modelUsageService.getModelUsageCount(memberId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalCount()).isEqualTo(0L);
        
        verify(modelUsageRepository).countModelUsageByMemberId(memberId);
    }

    private ModelUsageProjection createModelUsageProjection(Long adResultId, Long modelId, 
                                                           String modelName, String modelImageUrl, String prompt) {
        ModelUsageProjection projection = mock(ModelUsageProjection.class);
        given(projection.getAdResultId()).willReturn(adResultId);
        given(projection.getModelId()).willReturn(modelId);
        given(projection.getModelName()).willReturn(modelName);
        given(projection.getModelImageUrl()).willReturn(modelImageUrl);
        given(projection.getPrompt()).willReturn(prompt);
        given(projection.getCreatedAt()).willReturn(LocalDateTime.now());
        return projection;
    }
}