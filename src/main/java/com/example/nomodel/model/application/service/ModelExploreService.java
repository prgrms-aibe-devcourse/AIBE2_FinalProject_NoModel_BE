package com.example.nomodel.model.application.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.model.application.dto.ModelGalleryResponse;
import com.example.nomodel.model.application.dto.ModelCardResponse;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.OwnType;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        return convertToGalleryResponse(adminModels);
    }

    /**
     * 사용자 본인이 생성한 모델 목록 조회 (공개/비공개 모두 포함)
     */
    public ModelGalleryResponse getUserOwnedModels(Long userId) {
        List<AIModel> userModels = aiModelRepository.findByOwnerId(userId);
        return convertToGalleryResponse(userModels);
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

        return convertToGalleryResponse(allModels);
    }

    /**
     * 모델 목록을 갤러리 응답으로 변환 (썸네일 일괄 조회로 성능 최적화)
     */
    private ModelGalleryResponse convertToGalleryResponse(List<AIModel> models) {
        if (models.isEmpty()) {
            return ModelGalleryResponse.of(List.of());
        }
        
        // 모델 ID 목록 추출
        List<Long> modelIds = models.stream()
                .map(AIModel::getId)
                .toList();
        
        // 썸네일 파일 일괄 조회
        Map<Long, String> thumbnailMap = getThumbnailUrlMap(modelIds);
        
        // ModelCardResponse 변환
        List<ModelCardResponse> modelCards = models.stream()
                .map(model -> ModelCardResponse.of(
                        model.getId(),
                        model.getModelName(),
                        thumbnailMap.get(model.getId()),
                        model.getOwnType(),
                        model.isPubliclyAvailable(),
                        model.isHighResolutionModel(),
                        model.getCreatedAt(),
                        model.getUpdatedAt()
                ))
                .toList();
        
        return ModelGalleryResponse.of(modelCards);
    }
    
    /**
     * 여러 모델의 썸네일 URL을 일괄 조회 (N+1 쿼리 방지)
     * 우선순위: 1. 대표 이미지(isPrimary) 2. 썸네일(THUMBNAIL) 3. 일반 이미지
     */
    private Map<Long, String> getThumbnailUrlMap(List<Long> modelIds) {
        Map<Long, String> resultMap = new HashMap<>();
        
        // 1. 대표 이미지 우선 조회
        List<File> primaryFiles = fileRepository.findPrimaryImagesByModelIds(modelIds);
        Map<Long, String> primaryMap = primaryFiles.stream()
                .collect(Collectors.toMap(
                        File::getRelationId,
                        File::getFileUrl,
                        (existing, replacement) -> existing // 중복시 첫 번째 유지
                ));
        resultMap.putAll(primaryMap);
        
        // 2. 대표 이미지가 없는 모델들의 ID 찾기
        List<Long> noPrimaryModelIds = modelIds.stream()
                .filter(modelId -> !resultMap.containsKey(modelId))
                .toList();
        
        if (!noPrimaryModelIds.isEmpty()) {
            // 3. 썸네일 파일 조회
            List<File> thumbnailFiles = fileRepository.findThumbnailFilesByModelIds(noPrimaryModelIds);
            Map<Long, String> thumbnailMap = thumbnailFiles.stream()
                    .collect(Collectors.toMap(
                            File::getRelationId,
                            File::getFileUrl,
                            (existing, replacement) -> existing // 중복시 첫 번째 유지
                    ));
            resultMap.putAll(thumbnailMap);
            
            // 4. 썸네일도 없는 모델들의 ID 찾기
            List<Long> noThumbnailModelIds = noPrimaryModelIds.stream()
                    .filter(modelId -> !resultMap.containsKey(modelId))
                    .toList();
            
            // 5. 일반 이미지 파일 조회
            if (!noThumbnailModelIds.isEmpty()) {
                List<File> imageFiles = fileRepository.findImageFilesByModelIds(noThumbnailModelIds);
                Map<Long, String> imageMap = imageFiles.stream()
                        .collect(Collectors.toMap(
                                File::getRelationId,
                                File::getFileUrl,
                                (existing, replacement) -> existing // 중복시 첫 번째 유지
                        ));
                resultMap.putAll(imageMap);
            }
        }
        
        return resultMap;
    }
}