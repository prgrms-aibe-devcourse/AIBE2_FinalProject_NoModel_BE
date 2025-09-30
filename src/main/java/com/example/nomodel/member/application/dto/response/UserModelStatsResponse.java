package com.example.nomodel.member.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserModelStatsResponse(
        @Schema(description = "총 모델 수", example = "12")
        Long totalModelCount,

        @Schema(description = "모델 총 사용 횟수", example = "128")
        Long totalUsageCount,

        @Schema(description = "모델 평균 평점", example = "4.35")
        Double averageRating,

        @Schema(description = "공개된 모델 수", example = "5")
        Long publicModelCount
) {}
