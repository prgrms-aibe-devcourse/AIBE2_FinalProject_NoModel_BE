package com.example.nomodel.statistics.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.statistics.application.dto.response.DailyActivityDto;
import com.example.nomodel.statistics.application.dto.response.RatingDistributionDto;
import com.example.nomodel.statistics.application.dto.response.StatisticsMonthlyDto;
import com.example.nomodel.statistics.application.dto.response.StatisticsSummaryDto;
import com.example.nomodel.statistics.application.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatisticsController {
  
  private final StatisticsService statisticsService;
  
  @GetMapping("/admin/dashboard/summary")
  public ResponseEntity<?> getStatisticsSummary() {
    StatisticsSummaryDto result = statisticsService.getStatisticsSummary();
    return ResponseEntity.ok(ApiUtils.success(result));
  }
  
  @GetMapping("/admin/dashboard/monthly-stats")
  public ResponseEntity<?> getStatisticsMonthly() {
    List<StatisticsMonthlyDto> result = statisticsService.getStatisticsMonthly();
    return ResponseEntity.ok(ApiUtils.success(result));
  }

  @GetMapping("/admin/dashboard/daily-activity")
  public ResponseEntity<?> getDailyActivity() {
    List<DailyActivityDto> result = statisticsService.getDailyActivity();
    return ResponseEntity.ok(ApiUtils.success(result));
  }

  @GetMapping("/admin/dashboard/rating-distribution")
  public ResponseEntity<?> getRatingDistribution() {
    List<RatingDistributionDto> result = statisticsService.getRatingDistribution();
    return ResponseEntity.ok(ApiUtils.success(result));
  }
}
