package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.domain.document.AIModelDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AI 모델 검색 API 컨트롤러
 * Elasticsearch를 통한 AI 모델 검색 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/v1/models/search")
@RequiredArgsConstructor
@Tag(name = "AI Model Search", description = "AI 모델 검색 API")
public class AIModelSearchController {

    private final AIModelSearchService searchService;

    @Operation(summary = "AI 모델 통합 검색", description = "모델명, 설명, 태그에서 키워드 검색")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchModels(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("AI 모델 통합 검색 API 호출: keyword={}, page={}, size={}", keyword, page, size);

        Page<AIModelDocument> result = searchService.search(keyword, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "접근 가능한 AI 모델 검색", description = "사용자가 접근 가능한 모델 검색 (본인 모델 + 공개 모델)")
    @GetMapping("/accessible")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchAccessibleModels(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "사용자 ID") @RequestParam Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("접근 가능한 AI 모델 검색 API 호출: keyword={}, userId={}, page={}, size={}", 
                keyword, userId, page, size);

        Page<AIModelDocument> result = searchService.searchAccessibleModels(keyword, userId, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }


    @Operation(summary = "고급 검색", description = "키워드와 태그를 조합한 검색")
    @GetMapping("/advanced")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> advancedSearch(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "태그") @RequestParam String tag,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("AI 모델 고급 검색 API 호출: keyword={}, tag={}, page={}, size={}", 
                keyword, tag, page, size);

        Page<AIModelDocument> result = searchService.advancedSearch(keyword, tag, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "복합 필터 검색", description = "키워드, 태그, 가격 범위를 조합한 검색")
    @GetMapping("/filter")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchWithFilters(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "태그") @RequestParam String tag,
            @Parameter(description = "최소 가격") @RequestParam BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam BigDecimal maxPrice,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("AI 모델 복합 필터 검색 API 호출: keyword={}, tag={}, priceRange={}-{}", 
                keyword, tag, minPrice, maxPrice);

        Page<AIModelDocument> result = searchService.searchWithFilters(keyword, tag, 
                                                                      minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "태그별 검색", description = "특정 태그로 모델 검색")
    @GetMapping("/tag")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchByTag(
            @Parameter(description = "태그") @RequestParam String tag,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("태그별 AI 모델 검색 API 호출: tag={}, page={}, size={}", tag, page, size);

        Page<AIModelDocument> result = searchService.searchByTag(tag, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "소유자별 검색", description = "특정 소유자의 모델 검색")
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchByOwner(
            @Parameter(description = "소유자 ID") @PathVariable Long ownerId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("소유자별 AI 모델 검색 API 호출: ownerId={}, page={}, size={}", ownerId, page, size);

        Page<AIModelDocument> result = searchService.searchByOwner(ownerId, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "인기 모델 검색", description = "사용량과 평점 기준 인기 모델 검색")
    @GetMapping("/popular")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> getPopularModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("인기 AI 모델 검색 API 호출: page={}, size={}", page, size);

        Page<AIModelDocument> result = searchService.getPopularModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "최신 모델 검색", description = "최신 등록 순으로 모델 검색")
    @GetMapping("/recent")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> getRecentModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("최신 AI 모델 검색 API 호출: page={}, size={}", page, size);

        Page<AIModelDocument> result = searchService.getRecentModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "관리자 추천 모델", description = "관리자가 등록한 추천 모델 검색")
    @GetMapping("/recommended")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> getRecommendedModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("관리자 추천 AI 모델 검색 API 호출: page={}, size={}", page, size);

        Page<AIModelDocument> result = searchService.getRecommendedModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "고평점 모델 검색", description = "지정한 평점 이상의 모델 검색")
    @GetMapping("/high-rated")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> getHighRatedModels(
            @Parameter(description = "최소 평점") @RequestParam(defaultValue = "4.0") Double minRating,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("고평점 AI 모델 검색 API 호출: minRating={}, page={}, size={}", minRating, page, size);

        Page<AIModelDocument> result = searchService.getHighRatedModels(minRating, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "무료 모델 검색", description = "무료로 사용 가능한 모델 검색")
    @GetMapping("/free")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> getFreeModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("무료 AI 모델 검색 API 호출: page={}, size={}", page, size);

        Page<AIModelDocument> result = searchService.getFreeModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }


    @Operation(summary = "가격 범위 검색", description = "지정한 가격 범위 내의 모델 검색")
    @GetMapping("/price-range")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchByPriceRange(
            @Parameter(description = "최소 가격") @RequestParam BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam BigDecimal maxPrice,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("가격 범위 AI 모델 검색 API 호출: priceRange={}-{}, page={}, size={}", 
                minPrice, maxPrice, page, size);

        Page<AIModelDocument> result = searchService.searchByPriceRange(minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }


    @Operation(summary = "모델명 자동완성", description = "모델명 자동완성을 위한 제안 목록")
    @GetMapping("/suggestions")
    public ResponseEntity<ApiUtils.ApiResult<List<AIModelDocument>>> getModelNameSuggestions(
            @Parameter(description = "자동완성 접두사") @RequestParam String prefix) {

        log.info("AI 모델명 자동완성 API 호출: prefix={}", prefix);

        List<AIModelDocument> result = searchService.getModelNameSuggestions(prefix);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "유사 모델 검색", description = "특정 모델과 유사한 모델 검색")
    @GetMapping("/similar/{modelId}")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> getSimilarModels(
            @Parameter(description = "기준 모델 문서 ID") @PathVariable String modelId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("유사 AI 모델 검색 API 호출: modelId={}, page={}, size={}", modelId, page, size);

        Page<AIModelDocument> result = searchService.getSimilarModels(modelId, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "모델 상세 정보", description = "문서 ID로 모델 상세 정보 조회")
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiUtils.ApiResult<AIModelDocument>> getModelDetail(
            @Parameter(description = "모델 문서 ID") @PathVariable String documentId) {

        log.info("AI 모델 상세 정보 API 호출: documentId={}", documentId);

        Optional<AIModelDocument> result = searchService.findById(documentId);
        if (result.isPresent()) {
            return ResponseEntity.ok(ApiUtils.success(result.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "하이라이트 검색", description = "검색 결과에 하이라이트가 포함된 검색")
    @GetMapping("/highlight")
    public ResponseEntity<ApiUtils.ApiResult<Page<AIModelDocument>>> searchWithHighlight(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        log.info("하이라이트 AI 모델 검색 API 호출: keyword={}, page={}, size={}", keyword, page, size);

        Page<AIModelDocument> result = searchService.searchWithHighlight(keyword, page, size);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "모델 사용량 증가", description = "모델 사용 시 사용량 카운트 증가")
    @PostMapping("/{documentId}/usage")
    public ResponseEntity<ApiUtils.ApiResult<Void>> increaseUsage(
            @Parameter(description = "모델 문서 ID") @PathVariable String documentId) {

        log.info("AI 모델 사용량 증가 API 호출: documentId={}", documentId);

        searchService.increaseUsage(documentId);
        return ResponseEntity.ok(ApiUtils.success(null));
    }
}