package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.application.service.AIModelDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 모델 상세 조회 API 컨트롤러
 */
@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
@Tag(name = "AI Model Detail", description = "AI 모델 상세 조회 API")
public class AIModelDetailController {

    private final AIModelDetailService modelDetailService;

    @Operation(summary = "AI 모델 상세 조회", 
               description = "모델 ID로 상세 정보 조회 (자동으로 조회수 증가)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "모델을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{modelId}")
    public ResponseEntity<?> getModelDetail(
            @Parameter(description = "모델 ID") @PathVariable Long modelId) {
        
        // 상세 정보 조회 + 조회수 증가 통합
        AIModelDetailResponse response = modelDetailService.getModelDetailWithViewIncrement(modelId);
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }
}