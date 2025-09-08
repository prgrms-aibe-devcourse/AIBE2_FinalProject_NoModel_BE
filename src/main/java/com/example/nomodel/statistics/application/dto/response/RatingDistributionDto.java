package com.example.nomodel.statistics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class RatingDistributionDto {
    private int rating;
    private long count;
    private double percentage;
}
