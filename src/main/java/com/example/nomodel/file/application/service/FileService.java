package com.example.nomodel.file.application.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파일 응용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileJpaRepository fileRepository;

    /**
     * 여러 모델의 이미지 URL을 일괄 조회 (N+1 쿼리 방지)
     */
    public Map<Long, List<String>> getImageUrlsMap(List<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            return Map.of();
        }

        List<File> imageFiles = fileRepository.findImageFilesByModelIds(modelIds);

        return imageFiles.stream()
                .collect(Collectors.groupingBy(
                    File::getRelationId,
                    Collectors.mapping(File::getFileUrl, Collectors.toList())
                ));
    }
}