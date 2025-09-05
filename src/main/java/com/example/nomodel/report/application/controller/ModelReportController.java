package com.example.nomodel.report.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.report.application.dto.request.ModelReportRequest;
import com.example.nomodel.report.application.dto.response.ModelReportResponse;
import com.example.nomodel.report.application.service.ModelReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 모델 신고 API 컨트롤러
 * 신고 도메인 관점에서 모델 대상 신고를 처리
 */
@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Model Report", description = "AI 모델 신고 관리 API")
public class ModelReportController {

    private final ModelReportService modelReportService;

    @Operation(
        summary = "모델 신고", 
        description = "특정 AI 모델을 신고합니다. 중복 신고는 불가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "신고 접수 완료"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 신고, 유효하지 않은 데이터 등)"),
        @ApiResponse(responseCode = "404", description = "모델을 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/models/{modelId}")
    public ResponseEntity<?> createModelReport(
            @Parameter(description = "신고할 모델 ID", example = "1") 
            @PathVariable Long modelId,
            @Parameter(description = "신고 요청 정보") 
            @Valid @RequestBody ModelReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long reporterId = userDetails.getMemberId();
        
        log.info("모델 신고 요청: modelId={}, reporterId={}", modelId, reporterId);
        
        ModelReportResponse response = modelReportService.createModelReport(modelId, reporterId, request);
        
        log.info("모델 신고 처리 완료: reportId={}", response.getReportId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiUtils.success(response));
    }

    @Operation(
        summary = "모델의 신고 목록 조회", 
        description = "특정 모델에 대한 모든 신고 목록을 조회합니다. (관리자용)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "모델을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/models/{modelId}")
    public ResponseEntity<?> getModelReports(
            @Parameter(description = "조회할 모델 ID", example = "1") 
            @PathVariable Long modelId) {
        
        List<ModelReportResponse> reports = modelReportService.getModelReports(modelId);
        
        return ResponseEntity.ok(ApiUtils.success(reports));
    }

    @Operation(
        summary = "내 모델 신고 목록 조회", 
        description = "현재 로그인한 사용자가 신고한 모든 모델 신고 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my/models")
    public ResponseEntity<?> getMyModelReports(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long reporterId = userDetails.getMemberId();
        List<ModelReportResponse> reports = modelReportService.getUserModelReports(reporterId);
        
        return ResponseEntity.ok(ApiUtils.success(reports));
    }

    @Operation(
        summary = "특정 신고 상세 조회", 
        description = "신고 ID로 특정 신고의 상세 정보를 조회합니다. 신고자 본인만 조회 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getModelReport(
            @Parameter(description = "조회할 신고 ID", example = "1") 
            @PathVariable Long reportId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long reporterId = userDetails.getMemberId();
        ModelReportResponse report = modelReportService.getModelReport(reportId, reporterId);
        
        return ResponseEntity.ok(ApiUtils.success(report));
    }

    @Operation(
        summary = "모델 신고 통계 조회", 
        description = "특정 모델의 신고 관련 통계 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "모델을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/models/{modelId}/stats")
    public ResponseEntity<?> getModelReportStats(
            @Parameter(description = "조회할 모델 ID", example = "1") 
            @PathVariable Long modelId) {
        
        long totalReports = modelReportService.getTotalReportCount(modelId);
        long activeReports = modelReportService.getActiveReportCount(modelId);
        
        ModelReportStats stats = ModelReportStats.builder()
                .modelId(modelId)
                .totalReportCount(totalReports)
                .activeReportCount(activeReports)
                .build();
        
        return ResponseEntity.ok(ApiUtils.success(stats));
    }

    /**
     * 모델 신고 통계 응답 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ModelReportStats {
        @Parameter(description = "모델 ID")
        private Long modelId;
        
        @Parameter(description = "전체 신고 수")
        private long totalReportCount;
        
        @Parameter(description = "활성 신고 수 (처리 중)")
        private long activeReportCount;
    }
}