package com.example.nomodel.statistics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class DailyActivityDto {
    private String day;
    private long users;
    private long projects;
}
