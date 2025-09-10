package com.example.nomodel.model.application.dto.response;

import com.example.nomodel.model.domain.model.AdResult;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ModelUsageHistoryResponse(
    Long adResultId,
    Long modelId,
    String modelName,
    String prompt,
    String resultImageUrl,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {
    public static ModelUsageHistoryResponse from(AdResult adResult, String modelName, String resultImageUrl) {
        return ModelUsageHistoryResponse.builder()
            .adResultId(adResult.getId())
            .modelId(adResult.getModelId())
            .modelName(modelName)
            .prompt(adResult.getPrompt())
            .resultImageUrl(resultImageUrl)
            .createdAt(adResult.getCreatedAt())
            .build();
    }
}