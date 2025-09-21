package com.example.nomodel.model.command.application.dto.response;

import lombok.Builder;

@Builder
public record ModelUsageCountResponse(
    long totalCount
) {
    public static ModelUsageCountResponse from(long count) {
        return ModelUsageCountResponse.builder()
            .totalCount(count)
            .build();
    }
}