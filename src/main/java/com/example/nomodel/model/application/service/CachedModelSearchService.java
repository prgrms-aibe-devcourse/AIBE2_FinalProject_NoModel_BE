package com.example.nomodel.model.application.service;

import com.example.nomodel.model.application.dto.ModelSearchCacheKey;
import com.example.nomodel.model.domain.document.AIModelDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
     * 통합 검색 (캐싱 적용)
     * 처음 몇 페이지만 캐싱 (0-2 페이지)
     */
    @Cacheable(
            value = "modelSearch",
            key = "T(com.example.nomodel.model.application.dto.ModelSearchCacheKey).generate(#keyword, #isFree, #page, #size)",
            condition = "#page <= 2 && #size <= 20",  // 처음 3페이지, 페이지 크기 20 이하만 캐싱
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> search(String keyword, Boolean isFree, int page, int size) {
        log.debug("캐시 미스 - 검색 실행: keyword={}, isFree={}, page={}, size={}",
                keyword, isFree, page, size);
        return searchService.search(keyword, isFree, page, size);
    }

    /**
     * 인기 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "popularModels",
            key = "'popular_' + #page + '_' + #size",
            condition = "#page <= 4 && #size <= 20",
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
            condition = "#page <= 2 && #size <= 20",
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
            condition = "#page <= 2 && #size <= 20",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getRecommendedModels(int page, int size) {
        log.debug("캐시 미스 - 추천 모델 조회: page={}, size={}", page, size);
        return searchService.getRecommendedModels(page, size);
    }

    /**
     * 관리자 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "adminModels",
            key = "T(com.example.nomodel.model.application.dto.ModelSearchCacheKey).generate(#keyword, #isFree, #page, #size)",
            condition = "#page <= 2 && #size <= 20",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getAdminModels(String keyword, Boolean isFree, int page, int size) {
        log.debug("캐시 미스 - 관리자 모델 조회: keyword={}, isFree={}, page={}, size={}",
                keyword, isFree, page, size);
        return searchService.getAdminModels(keyword, isFree, page, size);
    }

    /**
     * 무료 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "freeModels",
            key = "'free_' + #page + '_' + #size",
            condition = "#page <= 4 && #size <= 20",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getFreeModels(int page, int size) {
        log.debug("캐시 미스 - 무료 모델 조회: page={}, size={}", page, size);
        return searchService.getFreeModels(page, size);
    }

    /**
     * 평점 높은 모델 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "modelSearch",
            key = "'highRated_' + #minRating + '_' + #page + '_' + #size",
            condition = "#page <= 2 && #size <= 20",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> getHighRatedModels(Double minRating, int page, int size) {
        log.debug("캐시 미스 - 평점 높은 모델 조회: minRating={}, page={}, size={}",
                minRating, page, size);
        return searchService.getHighRatedModels(minRating, page, size);
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
     * 태그 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "modelSearch",
            key = "'tag_' + #tag + '_' + #page + '_' + #size",
            condition = "#page <= 2 && #size <= 20",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> searchByTag(String tag, int page, int size) {
        log.debug("캐시 미스 - 태그 검색: tag={}, page={}, size={}", tag, page, size);
        return searchService.searchByTag(tag, page, size);
    }

    /**
     * 가격 범위 검색 (캐싱 적용)
     */
    @Cacheable(
            value = "modelSearch",
            key = "'price_' + #minPrice + '_' + #maxPrice + '_' + #page + '_' + #size",
            condition = "#page <= 1 && #size <= 20",
            unless = "#result == null || #result.isEmpty()"
    )
    public Page<AIModelDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        log.debug("캐시 미스 - 가격 범위 검색: min={}, max={}, page={}, size={}",
                minPrice, maxPrice, page, size);
        return searchService.searchByPriceRange(minPrice, maxPrice, page, size);
    }

    /**
     * 캐시 갱신 메서드
     * 모델이 업데이트되었을 때 해당 검색 캐시를 갱신
     */
    @CachePut(
            value = "modelSearch",
            key = "T(com.example.nomodel.model.application.dto.ModelSearchCacheKey).generate(#keyword, #isFree, #page, #size)"
    )
    public Page<AIModelDocument> refreshSearchCache(String keyword, Boolean isFree, int page, int size) {
        log.info("캐시 갱신 - 검색: keyword={}, isFree={}, page={}, size={}",
                keyword, isFree, page, size);
        return searchService.search(keyword, isFree, page, size);
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

    // 사용자별 검색은 캐싱하지 않음 (개인화된 데이터)
    public Page<AIModelDocument> getUserModels(String keyword, Boolean isFree, Long userId, int page, int size) {
        return searchService.getUserModels(keyword, isFree, userId, page, size);
    }

    public Page<AIModelDocument> searchAccessibleModels(String keyword, Long userId, int page, int size) {
        return searchService.searchAccessibleModels(keyword, userId, page, size);
    }

    // 유사 모델 검색은 캐싱하지 않음 (동적 데이터)
    public Page<AIModelDocument> getSimilarModels(String modelId, int page, int size) {
        return searchService.getSimilarModels(modelId, page, size);
    }
}