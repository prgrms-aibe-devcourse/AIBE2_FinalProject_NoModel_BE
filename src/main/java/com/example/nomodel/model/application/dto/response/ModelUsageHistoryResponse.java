package com.example.nomodel.model.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ModelUsageHistoryResponse(
    Long adResultId,
    Long modelId,
    String modelName,
    String modelImageUrl,
    String prompt,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt
) {
}