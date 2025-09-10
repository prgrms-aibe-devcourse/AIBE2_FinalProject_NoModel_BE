package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.request.AdResultRatingUpdateRequestDto;
import com.example.nomodel.model.application.dto.response.AdResultAverageRatingResponseDto;
import com.example.nomodel.model.application.dto.response.AdResultCountResponseDto;
import com.example.nomodel.model.application.dto.response.AdResultResponseDto;
import com.example.nomodel.model.application.service.AdResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AdResult", description = "AI 모델 결과물 관리")
@RestController
@RequestMapping("/api/ad-results")
@RequiredArgsConstructor
public class AdResultController {
    
    private final AdResultService adResultService;
    
    @Operation(summary = "내 결과물 목록 조회", description = "회원이 생성한 AI 모델 결과물 목록을 조회합니다")
    @GetMapping("/my")
    public ResponseEntity<ApiUtils.ApiResult<Page<AdResultResponseDto>>> getMyAdResults(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<AdResultResponseDto> results = adResultService.getMemberAdResults(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(ApiUtils.success(results));
    }
    
    @Operation(summary = "내 결과물 상세 조회", description = "특정 AI 모델 결과물의 상세 정보를 조회합니다")
    @GetMapping("/my/{adResultId}")
    public ResponseEntity<ApiUtils.ApiResult<AdResultResponseDto>> getMyAdResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "결과물 ID") @PathVariable Long adResultId) {
        
        AdResultResponseDto result = adResultService.getMemberAdResult(userDetails.getMemberId(), adResultId);
        return ResponseEntity.ok(ApiUtils.success(result));
    }
    
    @Operation(summary = "내 프로젝트 개수 조회", description = "회원이 생성한 총 프로젝트 개수를 조회합니다")
    @GetMapping("/my/count")
    public ResponseEntity<ApiUtils.ApiResult<AdResultCountResponseDto>> getMyProjectCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        AdResultCountResponseDto count = adResultService.getMemberProjectCount(userDetails.getMemberId());
        return ResponseEntity.ok(ApiUtils.success(count));
    }
    
    @Operation(summary = "내 평균 평점 조회", description = "회원이 작성한 평점들의 평균값을 조회합니다")
    @GetMapping("/my/average-rating")
    public ResponseEntity<ApiUtils.ApiResult<AdResultAverageRatingResponseDto>> getMyAverageRating(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        AdResultAverageRatingResponseDto rating = adResultService.getMemberAverageRating(userDetails.getMemberId());
        return ResponseEntity.ok(ApiUtils.success(rating));
    }
    
    @Operation(summary = "결과물 평점 작성/수정", description = "특정 AI 모델 결과물에 평점을 작성하거나 수정합니다")
    @PatchMapping("/my/{adResultId}/rating")
    public ResponseEntity<ApiUtils.ApiResult<String>> updateAdResultRating(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "결과물 ID") @PathVariable Long adResultId,
            @Valid @RequestBody AdResultRatingUpdateRequestDto request) {
        
        adResultService.updateAdResultRating(userDetails.getMemberId(), adResultId, request.memberRating());
        return ResponseEntity.ok(ApiUtils.success("평점이 성공적으로 업데이트되었습니다"));
    }
}