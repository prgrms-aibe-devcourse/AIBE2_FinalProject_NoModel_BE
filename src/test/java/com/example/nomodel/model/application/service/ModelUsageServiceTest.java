package com.example.nomodel.model.application.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.model.application.dto.response.ModelUsageCountResponse;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryResponse;
import com.example.nomodel.model.domain.model.AdResult;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.repository.AdResultJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
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
    private AdResultJpaRepository adResultRepository;
    
    @Mock
    private AIModelJpaRepository aiModelRepository;
    
    @Mock
    private FileJpaRepository fileRepository;
    
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
        
        AdResult adResult1 = createAdResult(1L, 100L, "테스트 프롬프트 1");
        AdResult adResult2 = createAdResult(2L, 200L, "테스트 프롬프트 2");
        List<AdResult> adResults = Arrays.asList(adResult1, adResult2);
        Page<AdResult> adResultPage = new PageImpl<>(adResults, pageable, 2);
        
        File resultFile1 = createFile(1L, "https://example.com/image1.jpg", "image/jpeg");
        File resultFile2 = createFile(2L, "https://example.com/image2.jpg", "image/png");
        List<File> resultFiles = Arrays.asList(resultFile1, resultFile2);
        
        AIModel aiModel1 = createAIModel(100L, "GPT-4");
        AIModel aiModel2 = createAIModel(200L, "DALL-E");
        List<AIModel> aiModels = Arrays.asList(aiModel1, aiModel2);
        
        given(adResultRepository.findByMemberIdOrderByCreatedAtDesc(eq(memberId), eq(pageable)))
            .willReturn(adResultPage);
        given(fileRepository.findByRelationTypeAndRelationIdIn(eq(RelationType.AD_RESULT), anyList()))
            .willReturn(resultFiles);
        given(aiModelRepository.findAllById(Arrays.asList(100L, 200L)))
            .willReturn(aiModels);
        
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
        assertThat(firstItem.prompt()).isEqualTo("테스트 프롬프트 1");
        assertThat(firstItem.resultImageUrl()).isEqualTo("https://example.com/image1.jpg");
        
        verify(adResultRepository).findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        verify(fileRepository).findByRelationTypeAndRelationIdIn(RelationType.AD_RESULT, Arrays.asList(1L, 2L));
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
        
        AdResult adResult = createAdResult(1L, modelId, "특정 모델 프롬프트");
        List<AdResult> adResults = Collections.singletonList(adResult);
        Page<AdResult> adResultPage = new PageImpl<>(adResults, pageable, 1);
        
        File resultFile = createFile(1L, "https://example.com/specific-image.jpg", "image/jpeg");
        List<File> resultFiles = Collections.singletonList(resultFile);
        
        AIModel aiModel = createAIModel(modelId, "GPT-4");
        List<AIModel> aiModels = Collections.singletonList(aiModel);
        
        given(adResultRepository.findByMemberIdAndModelIdOrderByCreatedAtDesc(eq(memberId), eq(modelId), eq(pageable)))
            .willReturn(adResultPage);
        given(fileRepository.findByRelationTypeAndRelationIdIn(eq(RelationType.AD_RESULT), anyList()))
            .willReturn(resultFiles);
        given(aiModelRepository.findAllById(Collections.singletonList(modelId)))
            .willReturn(aiModels);
        
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
        assertThat(item.resultImageUrl()).isEqualTo("https://example.com/specific-image.jpg");
        
        verify(adResultRepository).findByMemberIdAndModelIdOrderByCreatedAtDesc(memberId, modelId, pageable);
        verify(fileRepository).findByRelationTypeAndRelationIdIn(RelationType.AD_RESULT, Collections.singletonList(1L));
    }

    @Test
    @DisplayName("빈 사용 내역 조회 - 성공")
    void getModelUsageHistory_EmptyResults_Success() {
        // Given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        Page<AdResult> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        given(adResultRepository.findByMemberIdOrderByCreatedAtDesc(eq(memberId), eq(pageable)))
            .willReturn(emptyPage);
        given(fileRepository.findByRelationTypeAndRelationIdIn(eq(RelationType.AD_RESULT), eq(Collections.emptyList())))
            .willReturn(Collections.emptyList());
        
        // When
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(memberId, null, page, size);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        
        verify(adResultRepository).findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        verify(fileRepository).findByRelationTypeAndRelationIdIn(RelationType.AD_RESULT, Collections.emptyList());
    }

    @Test
    @DisplayName("이미지 파일이 없는 사용 내역 조회 - 성공")
    void getModelUsageHistory_NoImageFiles_Success() {
        // Given
        Long memberId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        
        AdResult adResult = createAdResult(1L, 100L, "이미지 없는 프롬프트");
        List<AdResult> adResults = Collections.singletonList(adResult);
        Page<AdResult> adResultPage = new PageImpl<>(adResults, pageable, 1);
        
        AIModel aiModel = createAIModel(100L, "GPT-4");
        List<AIModel> aiModels = Collections.singletonList(aiModel);
        
        given(adResultRepository.findByMemberIdOrderByCreatedAtDesc(eq(memberId), eq(pageable)))
            .willReturn(adResultPage);
        given(fileRepository.findByRelationTypeAndRelationIdIn(eq(RelationType.AD_RESULT), anyList()))
            .willReturn(Collections.emptyList());
        given(aiModelRepository.findAllById(Collections.singletonList(100L)))
            .willReturn(aiModels);
        
        // When
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(memberId, null, page, size);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        
        ModelUsageHistoryResponse item = response.content().get(0);
        assertThat(item.adResultId()).isEqualTo(1L);
        assertThat(item.resultImageUrl()).isNull();
        
        verify(adResultRepository).findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        verify(fileRepository).findByRelationTypeAndRelationIdIn(RelationType.AD_RESULT, Collections.singletonList(1L));
    }

    @Test
    @DisplayName("모델 사용 횟수 조회 - 성공")
    void getModelUsageCount_Success() {
        // Given
        Long memberId = 1L;
        long expectedCount = 15L;
        
        given(adResultRepository.countByMemberId(memberId))
            .willReturn(expectedCount);
        
        // When
        ModelUsageCountResponse response = modelUsageService.getModelUsageCount(memberId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalCount()).isEqualTo(expectedCount);
        
        verify(adResultRepository).countByMemberId(memberId);
    }

    @Test
    @DisplayName("모델 사용 횟수 조회 - 0건")
    void getModelUsageCount_ZeroCount() {
        // Given
        Long memberId = 1L;
        long expectedCount = 0L;
        
        given(adResultRepository.countByMemberId(memberId))
            .willReturn(expectedCount);
        
        // When
        ModelUsageCountResponse response = modelUsageService.getModelUsageCount(memberId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalCount()).isEqualTo(0L);
        
        verify(adResultRepository).countByMemberId(memberId);
    }

    private AdResult createAdResult(Long id, Long modelId, String prompt) {
        AdResult adResult = mock(AdResult.class);
        given(adResult.getId()).willReturn(id);
        given(adResult.getModelId()).willReturn(modelId);
        given(adResult.getPrompt()).willReturn(prompt);
        given(adResult.getCreatedAt()).willReturn(java.time.LocalDateTime.now());
        return adResult;
    }

    private File createFile(Long relationId, String fileUrl, String contentType) {
        return File.builder()
            .relationType(RelationType.AD_RESULT)
            .relationId(relationId)
            .fileUrl(fileUrl)
            .contentType(contentType)
            .build();
    }
    
    private AIModel createAIModel(Long id, String modelName) {
        AIModel aiModel = AIModel.builder()
            .modelName(modelName)
            .build();
        aiModel.setId(id);
        return aiModel;
    }
}