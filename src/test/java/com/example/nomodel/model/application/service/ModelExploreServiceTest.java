package com.example.nomodel.model.application.service;

import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.model.application.dto.ModelGalleryResponse;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.ModelMetadata;
import com.example.nomodel.model.domain.model.OwnType;
import com.example.nomodel.model.domain.model.SamplerType;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModelExploreService 단위 테스트")
class ModelExploreServiceTest {

    @Mock
    private AIModelJpaRepository aiModelRepository;

    @Mock
    private FileJpaRepository fileRepository;

    @InjectMocks
    private ModelExploreService modelExploreService;

    private AIModel adminModel;
    private AIModel userModel;
    private ModelMetadata modelMetadata;

    @BeforeEach
    void setUp() {
        modelMetadata = ModelMetadata.builder()
                .width(1024)
                .height(1024)
                .steps(20)
                .samplerIndex(SamplerType.DPM_PLUS_PLUS_2M_KARRAS)
                .nIter(1)
                .batchSize(1)
                .build();

        adminModel = AIModel.createAdminModel(
                "Admin Test Model",
                modelMetadata,
                BigDecimal.valueOf(100)
        );

        userModel = AIModel.createUserModel(
                "User Test Model",
                modelMetadata,
                1L
        );
    }

    @Test
    @DisplayName("관리자 모델 목록 조회 성공")
    void getAdminModels_Success() {
        // Given
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of(adminModel));
        given(fileRepository.findFirstThumbnailByRelation(any(), any()))
                .willReturn(Optional.empty());
        given(fileRepository.findImageFilesByRelation(any(), any()))
                .willReturn(List.of());

        // When
        ModelGalleryResponse response = modelExploreService.getAdminModels();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.models()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.adminModelCount()).isEqualTo(1);
        assertThat(response.userModelCount()).isEqualTo(0);
        assertThat(response.models().get(0).modelName()).isEqualTo("Admin Test Model");
        assertThat(response.models().get(0).ownType()).isEqualTo(OwnType.ADMIN);
    }

    @Test
    @DisplayName("사용자 본인 모델 목록 조회 성공")
    void getUserOwnedModels_Success() {
        // Given
        Long userId = 1L;
        given(aiModelRepository.findByOwnerId(userId))
                .willReturn(List.of(userModel));
        given(fileRepository.findFirstThumbnailByRelation(any(), any()))
                .willReturn(Optional.empty());
        given(fileRepository.findImageFilesByRelation(any(), any()))
                .willReturn(List.of());

        // When
        ModelGalleryResponse response = modelExploreService.getUserOwnedModels(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.models()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.adminModelCount()).isEqualTo(0);
        assertThat(response.userModelCount()).isEqualTo(1);
        assertThat(response.models().get(0).modelName()).isEqualTo("User Test Model");
        assertThat(response.models().get(0).ownType()).isEqualTo(OwnType.USER);
    }

    @Test
    @DisplayName("전체 접근 가능한 모델 목록 조회 성공")
    void getAllAccessibleModels_Success() {
        // Given
        Long userId = 1L;
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of(adminModel));
        given(aiModelRepository.findByOwnerId(userId))
                .willReturn(List.of(userModel));
        given(fileRepository.findFirstThumbnailByRelation(any(), any()))
                .willReturn(Optional.empty());
        given(fileRepository.findImageFilesByRelation(any(), any()))
                .willReturn(List.of());

        // When
        ModelGalleryResponse response = modelExploreService.getAllAccessibleModels(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.models()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.adminModelCount()).isEqualTo(1);
        assertThat(response.userModelCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("모델이 없을 때 빈 목록 반환")
    void getAdminModels_EmptyList() {
        // Given
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of());

        // When
        ModelGalleryResponse response = modelExploreService.getAdminModels();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.models()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.adminModelCount()).isEqualTo(0);
        assertThat(response.userModelCount()).isEqualTo(0);
    }
}