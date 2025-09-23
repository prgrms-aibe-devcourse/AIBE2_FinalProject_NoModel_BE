package com.example.nomodel.model.command.application.dto;

import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelStatistics;

/**
 * 배치 인덱싱용 모델 + 통계 + 리뷰 집계 Projection.
 */
public interface ModelIndexProjection {
    AIModel getModel();
    ModelStatistics getStatistics();
    String getOwnerName();
    Double getAverageRating();
    Long getReviewCount();
}
