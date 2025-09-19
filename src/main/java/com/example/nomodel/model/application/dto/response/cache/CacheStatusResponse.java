package com.example.nomodel.model.application.dto.response.cache;

import java.time.LocalDateTime;

/**
 * 전체 캐시 상태 응답 DTO
 */
public record CacheStatusResponse(
        LocalDateTime timestamp,
        SmartCacheStatusResponse smartCacheStatus,
        LazyInvalidationStatusResponse lazyInvalidationStatus,
        ModelCacheStatusResponse modelCacheStatus
) {
}