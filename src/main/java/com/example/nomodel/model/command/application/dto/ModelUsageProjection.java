package com.example.nomodel.model.command.application.dto;

import java.time.LocalDateTime;

/**
 * 모델 사용 내역 조회를 위한 Projection 인터페이스
 */
public interface ModelUsageProjection {
    Long getAdResultId();
    Long getModelId();
    String getModelName();
    String getModelImageUrl();
    String getPrompt();
    LocalDateTime getCreatedAt();
}