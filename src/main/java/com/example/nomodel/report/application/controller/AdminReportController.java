package com.example.nomodel.report.application.controller;


import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.report.application.dto.AdminReportSummaryDto;
import com.example.nomodel.report.application.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminReportController {
  
  private final AdminReportService adminReportService;
  
  @GetMapping("/admin/report/summary")
  public ResponseEntity<?> getReportSummary() {
    AdminReportSummaryDto reportSummary = adminReportService.getReportSummary();
    return ResponseEntity.ok(ApiUtils.success(reportSummary));
  }
}
