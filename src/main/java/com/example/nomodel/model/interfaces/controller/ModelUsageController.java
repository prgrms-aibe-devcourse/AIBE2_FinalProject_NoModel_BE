package com.example.nomodel.model.interfaces.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.application.service.ModelUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/model-usage")
@RequiredArgsConstructor
@Validated
@Tag(name = "Model Usage", description = "모델 사용 내역 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class ModelUsageController {

    private final ModelUsageService modelUsageService;

    @GetMapping("/history")
    @Operation(
        summary = "모델 사용 내역 조회",
        description = "인증된 회원의 모델 사용 내역을 페이지 단위로 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ModelUsageHistoryPageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ApiUtils.ApiResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 파라미터",
            content = @Content(schema = @Schema(implementation = ApiUtils.ApiResult.class))
        )
    })
    public ResponseEntity<ApiUtils.ApiResult<ModelUsageHistoryPageResponse>> getModelUsageHistory(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(description = "특정 모델 ID (선택사항)")
        @RequestParam(required = false) Long modelId,
        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("모델 사용 내역 조회 요청 - memberId: {}, modelId: {}, page: {}, size: {}", 
            userDetails.getMemberId(), modelId, page, size);
        
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(
            userDetails, modelId, page, size
        );
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @GetMapping("/count")
    @Operation(
        summary = "모델 사용 횟수 조회",
        description = "인증된 회원의 총 모델 사용 횟수를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = Long.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ApiUtils.ApiResult.class))
        )
    })
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Long>>> getModelUsageCount(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("모델 사용 횟수 조회 요청 - memberId: {}", userDetails.getMemberId());
        
        long count = modelUsageService.getModelUsageCount(userDetails);
        
        return ResponseEntity.ok(ApiUtils.success(Map.of("totalCount", count)));
    }
}