package com.example.nomodel.model.interfaces.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.response.ModelUsageCountResponse;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.application.service.ModelUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Model Usage API", description = "모델 사용 내역 관리 API")
@RestController
@RequestMapping("/members/me/models/usage")
@RequiredArgsConstructor
@Validated
public class ModelUsageController {

    private final ModelUsageService modelUsageService;

    @Operation(summary = "모델 사용 내역 조회", description = "인증된 회원의 모델 사용 내역을 페이지 단위로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiUtils.ApiResult<ModelUsageHistoryPageResponse>> getModelUsageHistory(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) Long modelId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        ModelUsageHistoryPageResponse response = modelUsageService.getModelUsageHistory(
            userDetails.getMemberId(), modelId, page, size
        );
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "모델 사용 횟수 조회", description = "인증된 회원의 총 모델 사용 횟수를 조회합니다.")
    @GetMapping("/count")
    public ResponseEntity<ApiUtils.ApiResult<ModelUsageCountResponse>> getModelUsageCount(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ModelUsageCountResponse response = modelUsageService.getModelUsageCount(userDetails.getMemberId());
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }
}