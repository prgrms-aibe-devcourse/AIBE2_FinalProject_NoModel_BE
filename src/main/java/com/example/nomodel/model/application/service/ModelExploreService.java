package com.example.nomodel.model.application.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.model.application.dto.ModelGalleryResponse;
import com.example.nomodel.model.application.dto.ModelCardResponse;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.OwnType;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelExploreService {

    private final AIModelJpaRepository aiModelRepository;
    private final FileJpaRepository fileRepository;

    /**
     * 관리자 모델 목록 조회 (공개된 ADMIN 타입 모델들)
     */
    public ModelGalleryResponse getAdminModels() {
        List<AIModel> adminModels = aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN);
        
        List<ModelCardResponse> modelCards = adminModels.stream()
                .map(this::convertToCardResponse)
                .toList();

        return ModelGalleryResponse.of(modelCards);
    }

    /**
     * 사용자 본인이 생성한 모델 목록 조회 (공개/비공개 모두 포함)
     */
    public ModelGalleryResponse getUserOwnedModels(Long userId) {
        List<AIModel> userModels = aiModelRepository.findByOwnerId(userId);
        
        List<ModelCardResponse> modelCards = userModels.stream()
                .map(this::convertToCardResponse)
                .toList();

        return ModelGalleryResponse.of(modelCards);
    }

    /**
     * 통합 모델 목록 조회 (관리자 모델 + 사용자 본인 모델)
     */
    public ModelGalleryResponse getAllAccessibleModels(Long userId) {
        // 관리자 모델 (공개된 것만)
        List<AIModel> adminModels = aiModelRepository.findByOwnTypeAndIsPublicTrue(OwnType.ADMIN);
        
        // 사용자 본인 모델 (공개/비공개 모두)
        List<AIModel> userModels = aiModelRepository.findByOwnerId(userId);
        
        // 결합
        List<AIModel> allModels = Stream.of(adminModels, userModels)
                .flatMap(List::stream)
                .toList();
        
        List<ModelCardResponse> modelCards = allModels.stream()
                .map(this::convertToCardResponse)
                .toList();

        return ModelGalleryResponse.of(modelCards);
    }

    /**
     * AIModel을 ModelCardResponse로 변환
     */
    private ModelCardResponse convertToCardResponse(AIModel model) {
        String thumbnailUrl = getThumbnailUrl(model.getId());
        
        return ModelCardResponse.of(
                model.getId(),
                model.getModelName(),
                thumbnailUrl,
                model.getOwnType(),
                model.isPubliclyAvailable(),
                model.isHighResolutionModel(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }

    /**
     * 모델의 썸네일 이미지 URL 조회
     */
    private String getThumbnailUrl(Long modelId) {
        // 먼저 썸네일 파일을 찾고, 없으면 일반 이미지 파일 중 첫 번째를 사용
        return fileRepository.findFirstThumbnailByRelation(RelationType.MODEL, modelId)
                .map(File::getFileUrl)
                .orElseGet(() -> 
                    fileRepository.findImageFilesByRelation(RelationType.MODEL, modelId)
                            .stream()
                            .findFirst()
                            .map(File::getFileUrl)
                            .orElse(null)
                );
    }
}