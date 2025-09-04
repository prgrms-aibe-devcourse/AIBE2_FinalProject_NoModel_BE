package com.example.nomodel.model.application.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
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
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

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
        given(fileRepository.findPrimaryImagesByModelIds(any()))
                .willReturn(List.of());
        given(fileRepository.findImageFilesByModelIds(any()))
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
        given(fileRepository.findPrimaryImagesByModelIds(any()))
                .willReturn(List.of());
        given(fileRepository.findImageFilesByModelIds(any()))
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
        given(fileRepository.findPrimaryImagesByModelIds(any()))
                .willReturn(List.of());
        given(fileRepository.findImageFilesByModelIds(any()))
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

    @Test
    @DisplayName("N+1 쿼리 방지를 위해 일괄 조회 메서드를 사용한다")
    void convertToGalleryResponse_BatchQuery_UsesCorrectMethods() {
        // Given
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of(adminModel));
        given(fileRepository.findPrimaryImagesByModelIds(any()))
                .willReturn(List.of());
        given(fileRepository.findImageFilesByModelIds(any()))
                .willReturn(List.of());

        // When
        ModelGalleryResponse response = modelExploreService.getAdminModels();

        // Then
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.models()).hasSize(1);

        // 일괄 조회 메서드가 호출되었는지 확인 (N+1 쿼리 방지)
        verify(fileRepository, times(1)).findPrimaryImagesByModelIds(any());
        verify(fileRepository, never()).findImageFilesByModelIds(any()); // 대표 이미지가 없을 때만 호출됨
        
        // 개별 조회 메서드는 호출되지 않았는지 확인
        verify(fileRepository, never()).findPrimaryByRelation(any(), any());
        verify(fileRepository, never()).findImageFilesByRelation(any(), any());
    }

    @Test
    @DisplayName("빈 모델 목록에서는 파일 조회를 하지 않는다")
    void convertToGalleryResponse_EmptyModels_SkipsFileQuery() {
        // Given
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of());

        // When
        ModelGalleryResponse response = modelExploreService.getAdminModels();

        // Then
        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.models()).isEmpty();

        // 파일 조회 메서드들이 호출되지 않았는지 확인
        verify(fileRepository, never()).findPrimaryImagesByModelIds(any());
        verify(fileRepository, never()).findImageFilesByModelIds(any());
        verify(fileRepository, never()).findPrimaryByRelation(any(), any());
        verify(fileRepository, never()).findImageFilesByRelation(any(), any());
    }

    @Test
    @DisplayName("대표 이미지가 없을 때 일반 이미지로 폴백한다")
    void getThumbnailUrlMap_FallbackToRegularImages_WhenNoPrimaryImages() {
        // Given
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of(adminModel));
        
        // 대표 이미지는 없음 
        given(fileRepository.findPrimaryImagesByModelIds(any()))
                .willReturn(List.of());
        
        // 일반 이미지는 있음
        File regularImageFile = mock(File.class);
        given(regularImageFile.getRelationId()).willReturn(adminModel.getId());
        given(regularImageFile.getFileUrl()).willReturn("http://example.com/regular-image.jpg");
        given(fileRepository.findImageFilesByModelIds(any()))
                .willReturn(List.of(regularImageFile));

        // When
        ModelGalleryResponse response = modelExploreService.getAdminModels();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.models()).hasSize(1);
        assertThat(response.models().get(0).thumbnailUrl()).isEqualTo("http://example.com/regular-image.jpg");

        // 대표 이미지 조회 후 일반 이미지로 폴백했는지 확인
        verify(fileRepository, times(1)).findPrimaryImagesByModelIds(any());
        verify(fileRepository, times(1)).findImageFilesByModelIds(any());
    }

    @Test
    @DisplayName("대표 이미지가 있을 때는 일반 이미지를 조회하지 않는다")
    void getThumbnailUrlMap_UsesPrimaryImages_SkipsRegularImages() {
        // Given
        given(aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN))
                .willReturn(List.of(adminModel));
        
        // 대표 이미지 있음
        File primaryImageFile = mock(File.class);
        given(primaryImageFile.getRelationId()).willReturn(adminModel.getId());
        given(primaryImageFile.getFileUrl()).willReturn("http://example.com/primary-image.jpg");
        given(fileRepository.findPrimaryImagesByModelIds(any()))
                .willReturn(List.of(primaryImageFile));

        // When
        ModelGalleryResponse response = modelExploreService.getAdminModels();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.models()).hasSize(1);
        assertThat(response.models().get(0).thumbnailUrl()).isEqualTo("http://example.com/primary-image.jpg");

        // 대표 이미지만 조회하고 일반 이미지는 조회하지 않았는지 확인
        verify(fileRepository, times(1)).findPrimaryImagesByModelIds(any());
        verify(fileRepository, never()).findImageFilesByModelIds(any());
    }
}