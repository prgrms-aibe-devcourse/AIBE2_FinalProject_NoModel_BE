package com.example.nomodel.model.command.application.dto.response.cache;

import java.time.LocalDateTime;

/**
 * 모델 캐시 서비스 상태 응답 DTO
 */
public record ModelCacheStatusResponse(
        String serviceName,
        String cacheImplementation,
        LocalDateTime lastChecked
) {
}