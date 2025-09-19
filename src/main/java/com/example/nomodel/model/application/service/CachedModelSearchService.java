package com.example.nomodel.model.application.service;

import com.example.nomodel.model.application.dto.response.cache.ModelSearchCacheKey;
import com.example.nomodel.model.domain.document.AIModelDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public Page<AIModelDocument> search(String keyword, Boolean isFree, int page, int size) {
        if (keyword == null) {
            log.debug("캐시 적용 - 기본 검색 실행: isFree={}, page={}, size={}", isFree, page, size);
        } else {
            log.debug("캐시 미적용 - 키워드 검색 실행: keyword={}, isFree={}, page={}, size={}", keyword, isFree, page, size);
        }
        return searchService.search(keyword, isFree, page, size);
    }

    /**
     * 인기 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "popularModels",
            key = "'popular_' + #page + '_' + #size",
            condition = "#page <= 2 && #size <= 10",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getPopularModels(int page, int size) {
        log.debug("캐시 미스 - 인기 모델 조회: page={}, size={}", page, size);
        return searchService.getPopularModels(page, size);
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
    public Page<AIModelDocument> getRecentModels(int page, int size) {
        log.debug("캐시 미스 - 최신 모델 조회: page={}, size={}", page, size);
        return searchService.getRecentModels(page, size);
    }

    /**
     * 추천 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "recommendedModels",
            key = "'recommended_' + #page + '_' + #size",
            condition = "#page <= 2 && #size <= 10",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getRecommendedModels(int page, int size) {
        log.debug("캐시 미스 - 추천 모델 조회: page={}, size={}", page, size);
        return searchService.getRecommendedModels(page, size);
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
    public Page<AIModelDocument> getAdminModels(String keyword, Boolean isFree, int page, int size) {
        if (keyword == null) {
            log.debug("캐시 적용 - 관리자 모델 조회: isFree={}, page={}, size={}", isFree, page, size);
        } else {
            log.debug("캐시 미적용 - 관리자 모델 키워드 검색: keyword={}, isFree={}, page={}, size={}", keyword, isFree, page, size);
        }
        return searchService.getAdminModels(keyword, isFree, page, size);
    }

    /**
     * 무료 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "freeModels",
            key = "'free_' + #page + '_' + #size",
            condition = "#page <= 2 && #size <= 10",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getFreeModels(int page, int size) {
        log.debug("캐시 미스 - 무료 모델 조회: page={}, size={}", page, size);
        return searchService.getFreeModels(page, size);
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
     * 인기 모델 캐시 갱신
     */
    @CachePut(
            value = "popularModels",
            key = "'popular_' + #page + '_' + #size"
    )
    public Page<AIModelDocument> refreshPopularModelsCache(int page, int size) {
        log.info("캐시 갱신 - 인기 모델: page={}, size={}", page, size);
        return searchService.getPopularModels(page, size);
    }

    /**
     * 최신 모델 캐시 갱신
     */
    @CachePut(
            value = "recentModels",
            key = "'recent_' + #page + '_' + #size"
    )
    public Page<AIModelDocument> refreshRecentModelsCache(int page, int size) {
        log.info("캐시 갱신 - 최신 모델: page={}, size={}", page, size);
        return searchService.getRecentModels(page, size);
    }

}