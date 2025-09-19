package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.PageResponse;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.application.service.CachedModelSearchService;
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
    private final CachedModelSearchService cachedSearchService;

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

        Page<AIModelDocument> result = cachedSearchService.search(keyword, isFree, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "관리자 모델 목록 조회/검색", description = "공개된 관리자 모델 목록 (ADMIN 타입). 키워드가 있으면 검색, 없으면 전체 조회")
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminModels(
            @Parameter(description = "검색 키워드 (선택적)") @RequestParam(required = false) String keyword,
            @Parameter(description = "가격 필터링 (true: 무료만, false: 유료만, null: 전체)") @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = cachedSearchService.getAdminModels(keyword, isFree, page, size);
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

        Page<AIModelDocument> result = cachedSearchService.getRecentModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }


    @Operation(summary = "모델명 자동완성", description = "모델명 자동완성을 위한 제안 목록 (completion suggester 기반)")
    @GetMapping("/suggestions")
    public ResponseEntity<?> getModelNameSuggestions(
            @Parameter(description = "자동완성 접두사") @RequestParam String prefix) {

        List<String> suggestions = cachedSearchService.getModelNameSuggestions(prefix);
        return ResponseEntity.ok(ApiUtils.success(suggestions));
    }

}