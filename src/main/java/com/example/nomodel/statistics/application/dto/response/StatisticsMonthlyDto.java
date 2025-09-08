package com.example.nomodel.statistics.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class StatisticsMonthlyDto {
  private String month;
  private long projects;
  private long revenue;
}
