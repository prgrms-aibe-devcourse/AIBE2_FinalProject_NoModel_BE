package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.PageResponse;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.domain.document.AIModelDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 모델 검색 API 컨트롤러
 * Elasticsearch를 통한 AI 모델 검색 기능 제공
 */
@RestController
@RequestMapping("/models/search")
@RequiredArgsConstructor
@Tag(name = "AI Model Search", description = "AI 모델 검색 API")
public class AIModelSearchController {

    private final AIModelSearchService searchService;

    @Operation(summary = "AI 모델 통합 검색", description = "모델명, 설명, 태그에서 키워드 검색. 키워드가 없으면 전체 공개 모델 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<?> searchModels(
            @Parameter(description = "검색 키워드 (선택적)") @RequestParam(required = false) String keyword,
            @Parameter(description = "가격 필터링 (true: 무료만, false: 유료만, null: 전체)") @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.search(keyword, isFree, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "관리자 모델 목록 조회/검색", description = "공개된 관리자 모델 목록 (ADMIN 타입). 키워드가 있으면 검색, 없으면 전체 조회")
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminModels(
            @Parameter(description = "검색 키워드 (선택적)") @RequestParam(required = false) String keyword,
            @Parameter(description = "가격 필터링 (true: 무료만, false: 유료만, null: 전체)") @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getAdminModels(keyword, isFree, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "내 모델 목록 조회/검색", description = "로그인한 사용자가 생성한 모델 목록. 키워드가 있으면 검색, 없으면 전체 조회")
    @GetMapping("/my-models")
    public ResponseEntity<?> getMyModels(
            @Parameter(description = "검색 키워드 (선택적)") @RequestParam(required = false) String keyword,
            @Parameter(description = "가격 필터링 (true: 무료만, false: 유료만, null: 전체)") @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getMemberId();
        Page<AIModelDocument> result = searchService.getUserModels(keyword, isFree, userId, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "접근 가능한 AI 모델 검색", description = "사용자가 접근 가능한 모델 검색 (본인 모델 + 공개 모델)")
    @GetMapping("/accessible")
    public ResponseEntity<?> searchAccessibleModels(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "사용자 ID") @RequestParam Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchAccessibleModels(keyword, userId, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }


    @Operation(summary = "인기 모델 검색", description = "사용량과 평점 기준 인기 모델 검색")
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getPopularModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }


    @Operation(summary = "고급 검색", description = "키워드와 태그를 결합한 고급 검색")
    @GetMapping("/advanced")
    public ResponseEntity<?> advancedSearch(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "태그") @RequestParam String tag,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.advancedSearch(keyword, tag, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "복합 필터 검색", description = "키워드, 태그, 가격 범위를 결합한 복합 필터 검색")
    @GetMapping("/filter")
    public ResponseEntity<?> searchWithFilters(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "태그") @RequestParam String tag,
            @Parameter(description = "최소 가격") @RequestParam java.math.BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam java.math.BigDecimal maxPrice,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchWithFilters(keyword, tag, minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "태그별 검색", description = "특정 태그로 모델 검색")
    @GetMapping("/tag")
    public ResponseEntity<?> searchByTag(
            @Parameter(description = "태그") @RequestParam String tag,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchByTag(tag, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "소유자별 검색", description = "특정 소유자의 모델 검색")
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<?> searchByOwner(
            @Parameter(description = "소유자 ID") @PathVariable Long ownerId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchByOwner(ownerId, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "최신 모델 검색", description = "최근 생성된 모델 검색")
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getRecentModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "관리자 추천 모델", description = "관리자가 추천하는 모델 목록")
    @GetMapping("/recommended")
    public ResponseEntity<?> getRecommendedModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getRecommendedModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "고평점 모델 검색", description = "높은 평점을 받은 모델 검색")
    @GetMapping("/high-rated")
    public ResponseEntity<?> getHighRatedModels(
            @Parameter(description = "최소 평점") @RequestParam(required = false) Double minRating,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        // 기본 최소 평점을 4.0으로 설정
        Double actualMinRating = (minRating != null) ? minRating : 4.0;
        Page<AIModelDocument> result = searchService.getHighRatedModels(actualMinRating, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "무료 모델 검색", description = "무료로 사용 가능한 모델 검색")
    @GetMapping("/free")
    public ResponseEntity<?> getFreeModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getFreeModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "가격 범위별 검색", description = "지정된 가격 범위 내의 모델 검색")
    @GetMapping("/price-range")
    public ResponseEntity<?> searchByPriceRange(
            @Parameter(description = "최소 가격") @RequestParam java.math.BigDecimal minPrice,
            @Parameter(description = "최대 가격") @RequestParam java.math.BigDecimal maxPrice,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchByPriceRange(minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "유사 모델 검색", description = "특정 모델과 유사한 모델 검색")
    @GetMapping("/similar/{modelId}")
    public ResponseEntity<?> getSimilarModels(
            @Parameter(description = "기준 모델 ID") @PathVariable String modelId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getSimilarModels(modelId, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "모델 상세 조회", description = "특정 모델의 상세 정보 조회")
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getModelById(
            @Parameter(description = "문서 ID") @PathVariable String documentId) {

        java.util.Optional<AIModelDocument> result = searchService.findById(documentId);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiUtils.success(result.get()));
    }

    @Operation(summary = "하이라이트 검색", description = "검색 결과에 하이라이트 적용")
    @GetMapping("/highlight")
    public ResponseEntity<?> searchWithHighlight(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchWithHighlight(keyword, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "모델 사용량 증가", description = "모델 사용량 카운트 증가")
    @PostMapping("/{documentId}/usage")
    public ResponseEntity<?> increaseUsage(
            @Parameter(description = "문서 ID") @PathVariable String documentId) {

        searchService.increaseUsage(documentId);
        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @Operation(summary = "모델명 자동완성", description = "모델명 자동완성을 위한 제안 목록 (completion suggester 기반)")
    @GetMapping("/suggestions")
    public ResponseEntity<?> getModelNameSuggestions(
            @Parameter(description = "자동완성 접두사") @RequestParam String prefix) {

        List<String> suggestions = searchService.getModelNameSuggestions(prefix);
        return ResponseEntity.ok(ApiUtils.success(suggestions));
    }

}