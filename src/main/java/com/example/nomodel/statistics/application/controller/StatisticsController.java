package com.example.nomodel.statistics.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.statistics.application.dto.response.StatisticsSummaryDto;
import com.example.nomodel.statistics.application.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatisticsController {
  
  private final StatisticsService statisticsService;
  
  @GetMapping("/admin/dashboard/summary")
  public ResponseEntity<?> getStatisticsSummary() {
    StatisticsSummaryDto result = statisticsService.getStatisticsSummary();
    return ResponseEntity.ok(ApiUtils.success(result));
  }
}
