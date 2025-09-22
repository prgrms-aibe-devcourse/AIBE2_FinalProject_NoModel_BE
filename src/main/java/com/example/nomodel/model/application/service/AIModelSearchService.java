package com.example.nomodel.model.application.service;

import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.ModelStatistics;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import com.example.nomodel.model.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.review.domain.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggester;
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
    private final ElasticsearchClient elasticsearchClient;
    private final ModelStatisticsJpaRepository modelStatisticsRepository;
    private final MemberJpaRepository memberRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 통합 검색 - 모델명, 설명, 태그에서 키워드 검색
     */
    public Page<AIModelDocument> search(String keyword, Boolean isFree, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("_score").descending());

        // 가격 필터링만 있는 경우
        if ((keyword == null || keyword.trim().isEmpty()) && isFree != null) {
            pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            if (isFree) {
                return searchRepository.findFreeModels(pageable);
            } else {
                return searchRepository.findPaidModels(pageable);
            }
        }

        // 키워드와 가격 필터링 조합
        if (keyword != null && !keyword.trim().isEmpty() && isFree != null) {
            if (isFree) {
                return searchRepository.searchFreeModelsWithKeyword(keyword, pageable);
            } else {
                return searchRepository.searchPaidModelsWithKeyword(keyword, pageable);
            }
        }

        // 키워드만 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            return searchRepository.searchByModelNameAndPrompt(keyword, pageable);
        }

        // 둘 다 없는 경우 - 전체 공개 모델 반환
        pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return searchRepository.findByIsPublic(true, pageable);
    }

    /**
     * 관리자 모델 목록 조회/검색 (공개된 ADMIN 타입 모델들)
     */
    public Page<AIModelDocument> getAdminModels(String keyword, Boolean isFree, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 가격 필터링만 있는 경우
        if ((keyword == null || keyword.trim().isEmpty()) && isFree != null) {
            if (isFree) {
                return searchRepository.searchFreeAdminModels(pageable);
            } else {
                return searchRepository.searchPaidAdminModels(pageable);
            }
        }

        // 키워드와 가격 필터링 조합
        if (keyword != null && !keyword.trim().isEmpty() && isFree != null) {
            pageable = PageRequest.of(page, size, Sort.by("_score").descending());
            if (isFree) {
                return searchRepository.searchFreeAdminModelsWithKeyword(keyword, pageable);
            } else {
                return searchRepository.searchPaidAdminModelsWithKeyword(keyword, pageable);
            }
        }

        // 키워드만 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            pageable = PageRequest.of(page, size, Sort.by("_score").descending());
            return searchRepository.searchInAdminModels(keyword, pageable);
        }

        // 둘 다 없는 경우 - 전체 관리자 모델 조회
        return searchRepository.findByOwnTypeAndIsPublic("ADMIN", true, pageable);
    }

    /**
     * 사용자 본인 모델 목록 조회/검색 (공개/비공개 모두)
     */
    public Page<AIModelDocument> getUserModels(String keyword, Boolean isFree, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 가격 필터링만 있는 경우
        if ((keyword == null || keyword.trim().isEmpty()) && isFree != null) {
            if (isFree) {
                return searchRepository.searchFreeUserModels(userId, pageable);
            } else {
                return searchRepository.searchPaidUserModels(userId, pageable);
            }
        }

        // 키워드와 가격 필터링 조합
        if (keyword != null && !keyword.trim().isEmpty() && isFree != null) {
            pageable = PageRequest.of(page, size, Sort.by("_score").descending());
            if (isFree) {
                return searchRepository.searchFreeUserModelsWithKeyword(keyword, userId, pageable);
            } else {
                return searchRepository.searchPaidUserModelsWithKeyword(keyword, userId, pageable);
            }
        }

        // 키워드만 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            pageable = PageRequest.of(page, size, Sort.by("_score").descending());
            return searchRepository.searchInUserModels(keyword, userId, pageable);
        }

        // 둘 다 없는 경우 - 전체 사용자 모델 조회
        return searchRepository.findByOwnerId(userId, pageable);
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

        Pageable pageable = PageRequest.of(page, size,
            Sort.by("usageCount").descending()
                .and(Sort.by("rating").descending()));
        return searchRepository.findPopularModels(pageable);
    }

    /**
     * 최신 모델 검색
     */
    public Page<AIModelDocument> getRecentModels(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return searchRepository.findRecentModels(pageable);
    }

    /**
     * 관리자 추천 모델 검색
     */
    public Page<AIModelDocument> getRecommendedModels(int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by("rating").descending()
                .and(Sort.by("usageCount").descending()));
        return searchRepository.findRecommendedModels(pageable);
    }

    /**
     * 평점 높은 모델 검색
     */
    public Page<AIModelDocument> getHighRatedModels(Double minRating, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by("rating").descending()
                .and(Sort.by("reviewCount").descending()));
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
     * 모델명 문자열 리스트만 반환
     */
    public List<String> getModelNameSuggestions(String prefix) {
        log.debug("모델명 자동완성 요청: prefix={}", prefix);

        try {
            // Completion Suggester 빌드
            Suggester suggester = Suggester.of(s -> s
                .suggesters("model-name-suggest", st -> st
                    .prefix(prefix)
                    .completion(c -> c
                        .field("suggest")
                        .size(10)
                        .skipDuplicates(true)
                        // 컨텍스트 필터링은 매핑에 contexts가 있는 경우만 사용
                        // .contexts(ctxs -> ctxs.category(cat -> cat.name("isPublic").contexts("true")))
                    )
                )
            );

            SearchRequest req = SearchRequest.of(b -> b
                .index("ai-models")
                .suggest(suggester)
                // _source를 아예 끄고 size를 0으로 설정
                .source(src -> src.fetch(false))
                .size(0)
            );

            SearchResponse<Void> resp = elasticsearchClient.search(req, Void.class);
            List<String> suggestions = resp.suggest().get("model-name-suggest").stream()
                .flatMap(s -> s.completion().options().stream())
                .map(o -> o.text())
                .distinct()
                .toList();

            log.debug("자동완성 결과: count={}, suggestions={}", suggestions.size(), suggestions);
            return suggestions;
        } catch (Exception e) {
            log.error("자동완성 검색 중 오류 발생: prefix={}", prefix, e);
            // 오류 발생 시 빈 리스트 반환
            return List.of();
        }
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

        Long usageCount = getUsageCount(aiModel);
        Long viewCount = getViewCount(aiModel);
        Double rating = getAverageRating(aiModel);
        Long reviewCount = getReviewCount(aiModel);

        AIModelDocument document = AIModelDocument.from(
            aiModel, ownerName, usageCount, viewCount, rating, reviewCount);
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

            Long usageCount = getUsageCount(updatedModel);
            Long viewCount = getViewCount(updatedModel);
            Double rating = getAverageRating(updatedModel);
            Long reviewCount = getReviewCount(updatedModel);

            AIModelDocument updatedDocument = AIModelDocument.from(
                updatedModel, ownerName, usageCount, viewCount, rating, reviewCount);
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

    /**
     * 모델의 사용량 조회
     */
    private Long getUsageCount(AIModel aiModel) {
        return modelStatisticsRepository.findByModelId(aiModel.getId())
                .map(ModelStatistics::getUsageCount)
                .orElse(0L);
    }

    /**
     * 모델의 조회수 조회
     */
    private Long getViewCount(AIModel aiModel) {
        return modelStatisticsRepository.findByModelId(aiModel.getId())
                .map(ModelStatistics::getViewCount)
                .orElse(0L);
    }

    /**
     * 모델의 평점 조회
     */
    private Double getAverageRating(AIModel aiModel) {
        return reviewRepository.calculateAverageRatingByModelId(aiModel.getId(), ReviewStatus.ACTIVE);
    }

    /**
     * 모델의 리뷰 수 조회
     */
    private Long getReviewCount(AIModel aiModel) {
        return reviewRepository.countByModelIdAndStatus(aiModel.getId(), ReviewStatus.ACTIVE);
    }

    /**
     * 모델의 리뷰 생성시 ID 조회(문자열 documentID를 숫자 modelId로 변환)
     */
    public Long getModelIdByDocumentId(String documentId) {
        // findById는 상속받은 메서드라서 바로 사용 가능
        Optional<AIModelDocument> document = searchRepository.findById(documentId);

        if (document.isPresent()) {
            return document.get().getModelId();
        } else {
            throw new RuntimeException("모델을 찾을 수 없습니다: " + documentId);
        }
    }
}