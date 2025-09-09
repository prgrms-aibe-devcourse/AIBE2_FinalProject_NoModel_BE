package com.example.nomodel.report.application.dto.response;

import com.example.nomodel.report.domain.model.Report;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 모델 신고 응답 DTO
 */
@Getter
@Builder
@Schema(description = "모델 신고 응답")
public class ModelReportResponse {

    @Schema(description = "신고 ID", example = "1")
    private Long reportId;

    @Schema(description = "신고 대상 타입", example = "MODEL")
    private String targetType;

    @Schema(description = "신고 대상 ID (모델 ID)", example = "123")
    private Long targetId;

    @Schema(description = "신고자 ID", example = "456")
    private Long reporterId;

    @Schema(description = "신고 상세 사유", example = "부적절한 콘텐츠가 포함되어 있습니다.")
    private String reasonDetail;

    @Schema(description = "신고 상태", example = "PENDING")
    private String reportStatus;

    @Schema(description = "신고 상태 설명", example = "접수")
    private String reportStatusDescription;

    @Schema(description = "관리자 메모")
    private String adminNote;

    @Schema(description = "신고 일시")
    private LocalDateTime createdAt;

    @Schema(description = "최종 수정일시")
    private LocalDateTime updatedAt;

    /**
     * Report 엔티티로부터 DTO 생성
     */
    public static ModelReportResponse from(Report report) {
        return ModelReportResponse.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType().getValue())
                .targetId(report.getTargetId())
                .reporterId(report.getReporterId())
                .reasonDetail(report.getReasonDetail())
                .reportStatus(report.getReportStatus().getValue())
                .reportStatusDescription(report.getReportStatus().getDescription())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    /**
     * 모델 신고만을 위한 간소화된 응답 생성
     */
    public static ModelReportResponse fromModelReport(Report report) {
        if (!report.getTargetType().isModelReport()) {
            throw new IllegalArgumentException("모델 신고가 아닙니다: " + report.getTargetType());
        }
        return from(report);
    }
}