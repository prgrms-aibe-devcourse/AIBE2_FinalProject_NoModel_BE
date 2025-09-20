package com.example.nomodel.model.application.service;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.model.application.dto.response.AIModelSearchResponse;
import com.example.nomodel.model.application.dto.response.cache.ModelSearchCacheKey;
import com.example.nomodel.model.domain.document.AIModelDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
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
@Slf4j
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
        if (keyword == null) {
            log.debug("캐시 적용 - 기본 검색 실행: isFree={}, page={}, size={}", isFree, page, size);
        } else {
            log.debug("캐시 미적용 - 키워드 검색 실행: keyword={}, isFree={}, page={}, size={}", keyword, isFree, page, size);
        }

        // 1. 모델 검색
        Page<AIModelDocument> models = searchService.search(keyword, isFree, page, size);

        // 2. 파일 정보 조합
        return combineWithFiles(models);
    }

    /**
     * 최신 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "recentModels",
            key = "'recent_' + #page + '_' + #size",
            condition = "#page <= 2 && #size <= 10",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelSearchResponse> getRecentModels(int page, int size) {
        log.debug("캐시 미스 - 최신 모델 조회: page={}, size={}", page, size);
        Page<AIModelDocument> models = searchService.getRecentModels(page, size);
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
        if (keyword == null) {
            log.debug("캐시 적용 - 관리자 모델 조회: isFree={}, page={}, size={}", isFree, page, size);
        } else {
            log.debug("캐시 미적용 - 관리자 모델 키워드 검색: keyword={}, isFree={}, page={}, size={}", keyword, isFree, page, size);
        }
        Page<AIModelDocument> models = searchService.getAdminModels(keyword, isFree, page, size);
        return combineWithFiles(models);
    }

    /**
     * 자동완성 제안 (캐싱 적용)
     */
    @Cacheable(
            value = "autoComplete",
            key = "#prefix.toLowerCase()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<String> getModelNameSuggestions(String prefix) {
        log.debug("캐시 미스 - 자동완성 조회: prefix={}", prefix);
        return searchService.getModelNameSuggestions(prefix);
    }

    /**
     * 최신 모델 캐시 갱신
     */
    @CachePut(
            value = "recentModels",
            key = "'recent_' + #page + '_' + #size"
    )
    public Page<AIModelSearchResponse> refreshRecentModelsCache(int page, int size) {
        log.info("캐시 갱신 - 최신 모델: page={}, size={}", page, size);
        Page<AIModelDocument> models = searchService.getRecentModels(page, size);
        return combineWithFiles(models);
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