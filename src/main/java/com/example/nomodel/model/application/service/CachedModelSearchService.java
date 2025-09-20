package com.example.nomodel.model.application.service;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.model.application.dto.response.AIModelSearchResponse;
import com.example.nomodel.model.domain.model.document.AIModelDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 캐시가 적용된 AI 모델 검색 서비스
 * 자주 조회되는 검색 결과를 Redis에 캐싱하여 성능 향상
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CachedModelSearchService {

    private final AIModelSearchService searchService;
    private final FileService fileService;

    /**
     * 통합 검색 (키워드 없는 기본 검색만 캐싱)
     * 키워드가 있는 검색은 인기 검색어 통계 구현 후 적용 예정
     */
    @Cacheable(
            value = "modelSearch",
            key = "T(com.example.nomodel.model.application.dto.response.cache.ModelSearchCacheKey).generate(#keyword, #isFree, #page, #size)",
            condition = "#keyword == null && #page <= 2 && #size <= 10",  // 키워드 없는 기본 검색만 캐싱
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelSearchResponse> search(String keyword, Boolean isFree, int page, int size) {
        // 1. 모델 검색
        Page<AIModelDocument> models = searchService.search(keyword, isFree, page, size);

        // 2. 파일 정보 조합
        return combineWithFiles(models);
    }


    /**
     * 관리자 모델 검색 (키워드 없는 기본 검색만 캐싱)
     */
    @Cacheable(
            value = "adminModels",
            key = "T(com.example.nomodel.model.application.dto.response.cache.ModelSearchCacheKey).generate(#keyword, #isFree, #page, #size)",
            condition = "#keyword == null && #page <= 2 && #size <= 10",  // 키워드 없는 기본 검색만 캐싱
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelSearchResponse> getAdminModels(String keyword, Boolean isFree, int page, int size) {
        Page<AIModelDocument> models = searchService.getAdminModels(keyword, isFree, page, size);
        return combineWithFiles(models);
    }

    /**
     * 자동완성 제안 (캐싱 미적용 - 메모리 효율성)
     */
    public List<String> getModelNameSuggestions(String prefix) {
        return searchService.getModelNameSuggestions(prefix);
    }


    /**
     * AIModelDocument 페이지와 파일 정보를 조합하여 AIModelSearchResponse 페이지로 변환
     */
    private Page<AIModelSearchResponse> combineWithFiles(Page<AIModelDocument> models) {
        if (models.isEmpty()) {
            return Page.empty();
        }

        // 1. 모든 모델 ID 추출
        List<Long> modelIds = models.getContent().stream()
                .map(AIModelDocument::getModelId)
                .toList();

        // 2. 배치로 파일 URL 조회 (N+1 쿼리 방지)
        Map<Long, List<String>> imageUrlsMap = fileService.getImageUrlsMap(modelIds);

        // 3. AIModelDocument + 파일 정보 조합
        List<AIModelSearchResponse> responses = models.getContent().stream()
                .map(document -> {
                    List<String> imageUrls = imageUrlsMap.getOrDefault(document.getModelId(), List.of());
                    return AIModelSearchResponse.from(document, imageUrls);
                })
                .toList();

        // 4. Page 객체로 변환하여 반환
        return new PageImpl<>(responses, models.getPageable(), models.getTotalElements());
    }
}