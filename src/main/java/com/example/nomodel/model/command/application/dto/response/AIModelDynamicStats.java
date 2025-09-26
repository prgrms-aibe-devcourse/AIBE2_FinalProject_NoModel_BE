package com.example.nomodel.model.command.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIModelDynamicStats {
    private Double avgRating;
    private Long reviewCount;
    private Long usageCount;
    private Long viewCount;
}
