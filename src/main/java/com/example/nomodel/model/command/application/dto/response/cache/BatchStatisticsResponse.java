package com.example.nomodel.model.command.application.dto.response.cache;

import java.time.LocalDateTime;

/**
 * 배치 처리 통계 응답 DTO
 */
public record BatchStatisticsResponse(
        Long searchCacheProcessedCount,
        LocalDateTime searchCacheLastRun,
        Long modelCacheProcessedCount,
        LocalDateTime modelCacheLastRun
) {
}