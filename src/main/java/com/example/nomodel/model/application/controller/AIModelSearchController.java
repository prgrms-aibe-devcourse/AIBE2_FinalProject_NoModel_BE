package com.example.nomodel.model.application.controller;

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
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.search(keyword, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "관리자 모델 목록 조회", description = "공개된 관리자 모델 목록 (ADMIN 타입)")
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminModels(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getAdminModels(page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

    @Operation(summary = "내 모델 목록 조회", description = "사용자가 생성한 모든 모델 목록")
    @GetMapping("/my-models")
    public ResponseEntity<?> getUserModels(
            @Parameter(description = "사용자 ID") @RequestParam Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.getUserModels(userId, page, size);
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


    @Operation(summary = "모델명 자동완성", description = "모델명 자동완성을 위한 제안 목록 (completion suggester 기반)")
    @GetMapping("/suggestions")
    public ResponseEntity<?> getModelNameSuggestions(
            @Parameter(description = "자동완성 접두사") @RequestParam String prefix) {

        List<AIModelDocument> result = searchService.getModelNameSuggestions(prefix);
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    @Operation(summary = "부분 모델명 검색", description = "모델명 부분 검색 (edge n-gram 기반)")
    @GetMapping("/partial")
    public ResponseEntity<?> searchByPartialName(
            @Parameter(description = "부분 검색어") @RequestParam String partial,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {

        Page<AIModelDocument> result = searchService.searchByPartialName(partial, page, size);
        return ResponseEntity.ok(ApiUtils.success(PageResponse.from(result)));
    }

}