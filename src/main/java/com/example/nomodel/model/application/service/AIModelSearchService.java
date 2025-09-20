package com.example.nomodel.model.application.service;

import com.example.nomodel.model.domain.model.document.AIModelDocument;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
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

import java.util.List;

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
     * 관리자 추천 모델 검색
     */
    public Page<AIModelDocument> getRecommendedModels(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, 
            Sort.by("rating").descending()
                .and(Sort.by("usageCount").descending()));
        return searchRepository.findRecommendedModels(pageable);
    }


    /**
     * 무료 모델 검색
     */
    public Page<AIModelDocument> getFreeModels(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("rating").descending());
        return searchRepository.findFreeModels(pageable);
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
.map(co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption::text)
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
}