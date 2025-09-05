package com.example.nomodel.report.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모델 신고 요청 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "모델 신고 요청")
public class ModelReportRequest {

    @NotBlank(message = "신고 사유는 필수입니다")
    @Size(max = 1000, message = "신고 사유는 1000자를 초과할 수 없습니다")
    @Schema(description = "신고 상세 사유", example = "부적절한 콘텐츠가 포함되어 있습니다.", maxLength = 1000, requiredMode = Schema.RequiredMode.REQUIRED)
    private String reasonDetail;

    public ModelReportRequest(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }
}