package com.example.nomodel.model.command.application.dto;

import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelStatistics;

/**
 * AI 모델과 통계 정보를 함께 조회하기 위한 Projection
 * JPA 쿼리 최적화를 위한 인터페이스 기반 DTO
 */
public interface ModelWithStatisticsProjection {
    AIModel getModel();
    ModelStatistics getStatistics();
    String getOwnerName();
}