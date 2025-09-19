package com.example.nomodel.report.application.controller;


import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.report.application.dto.request.ProcessReportDto;
import com.example.nomodel.report.application.dto.response.AdminReportDto;
import com.example.nomodel.report.application.dto.response.AdminReportSummaryDto;
import com.example.nomodel.report.application.service.AdminReportService;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminReportController {
  
  private final AdminReportService adminReportService;
  
  @GetMapping("/admin/report/summary")
  public ResponseEntity<?> getReportSummary() {
    AdminReportSummaryDto reportSummary = adminReportService.getReportSummary();
    return ResponseEntity.ok(ApiUtils.success(reportSummary));
  }
  
  @GetMapping("/admin/report")
  public ResponseEntity<?> getReport(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size,
                                     @RequestParam(name = "targetType", required = false) String targetType,
                                     @RequestParam(name = "reportStatus", required = false) String reportStatus) {
    TargetType tt     = parseEnumOrNull(TargetType.class, targetType);
    ReportStatus rs   = parseEnumOrNull(ReportStatus.class, reportStatus);
    
    Page<AdminReportDto> reportList = adminReportService.getReport(page, size, tt, rs);
    return ResponseEntity.ok(ApiUtils.success(reportList));
  }
  
  @GetMapping("/admin/report/{reportId}")
  public ResponseEntity<?> getReportDetail(@PathVariable String reportId) {
    AdminReportDto reportDetail = adminReportService.getReportDetail(reportId);
    return ResponseEntity.ok(ApiUtils.success(reportDetail));
  }
  
  @PatchMapping("/admin/report/{reportId}/process")
  public ResponseEntity<?> process(
          @PathVariable Long reportId,
          @RequestBody ProcessReportDto request
  ) {
    AdminReportDto result = adminReportService.processReport(reportId, request);
    return ResponseEntity.ok(ApiUtils.success(result));
  }
  
  private static <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String raw) {
    if (raw == null) return null;                          // 파라미터 자체가 없음
    String s = raw.trim();
    if (s.isEmpty() || "null".equalsIgnoreCase(s) || "all".equalsIgnoreCase(s)) return null;
    try { return Enum.valueOf(type, s.toUpperCase(java.util.Locale.ROOT)); }
    catch (IllegalArgumentException e) {
      throw new ApplicationException(ErrorCode.INVALID_ENUM_VALUE);
    }
  }
}
