package com.example.nomodel.model.application.service;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AI 모델 검색 서비스
 * Elasticsearch를 통한 AI 모델 검색 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIModelSearchService {

    private final AIModelSearchRepository searchRepository;

    /**
     * 통합 검색 - 모델명, 설명, 태그에서 키워드 검색
     */
    public Page<AIModelDocument> search(String keyword, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        
        // 빈 키워드일 때는 전체 공개 모델 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return searchRepository.findByIsPublic(true, pageable);
        }
        
        return searchRepository.searchByModelNameAndPrompt(keyword, pageable);
    }

    /**
     * 사용자 접근 가능한 모델 검색 (본인 모델 + 공개 모델)
     */
    public Page<AIModelDocument> searchAccessibleModels(String keyword, Long userId, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            return searchRepository.searchAccessibleModels(keyword, userId, pageable);
        } else {
            return searchRepository.findAccessibleModels(userId, pageable);
        }
    }


    /**
     * 고급 검색 - 태그와 키워드 조합
     */
    public Page<AIModelDocument> advancedSearch(String keyword, String tag, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        return searchRepository.searchWithMultipleFilters(keyword, tag, BigDecimal.ZERO, new BigDecimal("999999"), pageable);
    }

    /**
     * 복합 필터 검색 - 태그와 가격 범위
     */
    public Page<AIModelDocument> searchWithFilters(String keyword, String tag, 
                                                  BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        return searchRepository.searchWithMultipleFilters(keyword, tag, minPrice, maxPrice, pageable);
    }

    /**
     * 태그로 검색
     */
    public Page<AIModelDocument> searchByTag(String tag, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return searchRepository.searchByTag(tag, pageable);
    }

    /**
     * 소유자별 검색
     */
    public Page<AIModelDocument> searchByOwner(Long ownerId, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return searchRepository.findByOwnerId(ownerId, pageable);
    }

    /**
     * 인기 모델 검색 (사용량 + 평점 기준)
     */
    public Page<AIModelDocument> getPopularModels(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return searchRepository.findPopularModels(pageable);
    }

    /**
     * 최신 모델 검색
     */
    public Page<AIModelDocument> getRecentModels(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return searchRepository.findRecentModels(pageable);
    }

    /**
     * 관리자 추천 모델 검색
     */
    public Page<AIModelDocument> getRecommendedModels(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return searchRepository.findRecommendedModels(pageable);
    }

    /**
     * 평점 높은 모델 검색
     */
    public Page<AIModelDocument> getHighRatedModels(Double minRating, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return searchRepository.findHighRatedModels(minRating, pageable);
    }

    /**
     * 무료 모델 검색
     */
    public Page<AIModelDocument> getFreeModels(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("rating").descending());
        return searchRepository.findFreeModels(pageable);
    }

    /**
     * 가격 범위로 검색
     */
    public Page<AIModelDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("price").ascending());
        return searchRepository.searchByPriceRange(minPrice, maxPrice, pageable);
    }


    /**
     * 자동완성 제안 (completion suggester 기반)
     */
    public List<AIModelDocument> getModelNameSuggestions(String prefix) {
        return searchRepository.findModelNameSuggestions(prefix);
    }

    /**
     * 부분 모델명 검색 (edge n-gram 기반)
     */
    public Page<AIModelDocument> searchByPartialName(String partial, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        return searchRepository.searchByPartialName(partial, pageable);
    }

    /**
     * 유사 모델 검색
     */
    public Page<AIModelDocument> getSimilarModels(String modelId, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return searchRepository.findSimilarModels(modelId, pageable);
    }

    /**
     * 모델 ID로 검색
     */
    public Optional<AIModelDocument> findById(String documentId) {
        return searchRepository.findById(documentId);
    }

    /**
     * 하이라이트 기능을 포함한 검색
     */
    public Page<AIModelDocument> searchWithHighlight(String keyword, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());
        return searchRepository.searchWithHighlight(keyword, pageable);
    }

    /**
     * AI 모델을 Elasticsearch에 색인
     */
    @Transactional
    public AIModelDocument indexModel(AIModel aiModel, String ownerName) {
        log.info("AI 모델 색인: modelId={}, modelName={}", aiModel.getId(), aiModel.getModelName());
        
        AIModelDocument document = AIModelDocument.from(aiModel, ownerName);
        return searchRepository.save(document);
    }

    /**
     * AI 모델 문서 업데이트
     */
    @Transactional
    public Optional<AIModelDocument> updateModel(Long modelId, AIModel updatedModel, String ownerName) {
        log.info("AI 모델 문서 업데이트: modelId={}", modelId);
        
        // 기존 문서 찾기 (modelId로)
        Page<AIModelDocument> existingDocs = searchRepository.findByOwnerId(updatedModel.getOwnerId(), 
                PageRequest.of(0, 1));
        
        if (!existingDocs.isEmpty()) {
            AIModelDocument existingDoc = existingDocs.getContent().get(0);
            AIModelDocument updatedDocument = AIModelDocument.from(updatedModel, ownerName);
            // ID는 기존 것을 유지
            return Optional.of(searchRepository.save(updatedDocument));
        }
        
        // 기존 문서가 없으면 새로 생성
        return Optional.of(indexModel(updatedModel, ownerName));
    }

    /**
     * 사용량 증가
     */
    @Transactional
    public void increaseUsage(String documentId) {
        searchRepository.findById(documentId)
                .ifPresent(document -> {
                    document.increaseUsage();
                    searchRepository.save(document);
                    log.debug("AI 모델 사용량 증가: documentId={}, usageCount={}", 
                            documentId, document.getUsageCount());
                });
    }

    /**
     * 평점 업데이트
     */
    @Transactional
    public void updateRating(String documentId, Double rating, Long reviewCount) {
        searchRepository.findById(documentId)
                .ifPresent(document -> {
                    document.updateRating(rating, reviewCount);
                    searchRepository.save(document);
                    log.debug("AI 모델 평점 업데이트: documentId={}, rating={}, reviewCount={}", 
                            documentId, rating, reviewCount);
                });
    }

    /**
     * 공개 상태 변경
     */
    @Transactional
    public void updateVisibility(String documentId, Boolean isPublic) {
        searchRepository.findById(documentId)
                .ifPresent(document -> {
                    document.updateVisibility(isPublic);
                    searchRepository.save(document);
                    log.debug("AI 모델 공개 상태 변경: documentId={}, isPublic={}", 
                            documentId, isPublic);
                });
    }

    /**
     * 가격 업데이트
     */
    @Transactional
    public void updatePrice(String documentId, BigDecimal price) {
        searchRepository.findById(documentId)
                .ifPresent(document -> {
                    document.updatePrice(price);
                    searchRepository.save(document);
                    log.debug("AI 모델 가격 업데이트: documentId={}, price={}", 
                            documentId, price);
                });
    }

    /**
     * 문서 삭제
     */
    @Transactional
    public void deleteModel(String documentId) {
        log.info("AI 모델 문서 삭제: documentId={}", documentId);
        searchRepository.deleteById(documentId);
    }
}