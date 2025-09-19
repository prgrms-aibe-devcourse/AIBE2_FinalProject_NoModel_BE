package com.example.nomodel.model.application.dto.response.cache;

import java.time.LocalDateTime;

/**
 * 지연 무효화 서비스 상태 응답 DTO
 */
public record LazyInvalidationStatusResponse(
        String serviceName,
        Long dirtySearchCacheCount,
        Long dirtyModelCacheCount,
        BatchStatisticsResponse batchStatistics,
        LocalDateTime lastChecked
) {
}