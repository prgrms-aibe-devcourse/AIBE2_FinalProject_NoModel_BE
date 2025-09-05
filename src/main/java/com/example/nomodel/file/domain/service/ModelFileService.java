package com.example.nomodel.file.domain.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 모델 파일 도메인 서비스
 * 
 * Domain Service가 필요한 이유:
 * 1. 파일과 모델 간의 관계에 대한 도메인 지식 캡슐화
 * 2. 비즈니스 규칙: 모델은 이미지 파일만 관리
 * 3. N+1 쿼리 방지를 위한 일괄 조회 로직
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ModelFileService {

    private final FileJpaRepository fileRepository;

    /**
     * 모델의 모든 이미지 파일 조회
     * 프론트엔드에서 isPrimary 값으로 썸네일/프리뷰 구분
     */
    public List<File> getModelFiles(Long modelId) {
        return fileRepository.findImageFilesByRelation(RelationType.MODEL, modelId);
    }

    /**
     * 여러 모델의 모든 이미지 파일들을 일괄 조회 (N+1 쿼리 방지)
     * 검색 결과 목록이나 여러 모델을 동시에 처리할 때 사용
     */
    public List<File> getModelsFiles(List<Long> modelIds) {
        return fileRepository.findImageFilesByModelIds(modelIds);
    }

    /**
     * 모델에 파일이 있는지 확인
     * 비즈니스 규칙: 모델은 최소 하나의 이미지를 가져야 함
     */
    public boolean hasModelFiles(Long modelId) {
        return fileRepository.countByRelationTypeAndRelationId(RelationType.MODEL, modelId) > 0;
    }
}