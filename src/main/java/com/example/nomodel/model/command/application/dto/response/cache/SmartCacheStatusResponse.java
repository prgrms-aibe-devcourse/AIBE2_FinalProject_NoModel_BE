package com.example.nomodel.model.command.application.dto.response.cache;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 스마트 캐시 무효화 서비스 상태 응답 DTO
 */
public record SmartCacheStatusResponse(
        String serviceName,
        Long dirtySearchCacheCount,
        List<String> dirtySearchCaches,
        LocalDateTime lastChecked
) {
}