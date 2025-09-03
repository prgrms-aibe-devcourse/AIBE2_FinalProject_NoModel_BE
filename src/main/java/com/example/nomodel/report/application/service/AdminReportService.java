package com.example.nomodel.report.application.service;

import com.example.nomodel.report.application.dto.AdminReportSummaryDto;
import com.example.nomodel.report.domain.repository.ReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminReportService {
  private final ReportJpaRepository reportJpaRepository;
  
  public AdminReportSummaryDto getReportSummary() {
    return reportJpaRepository.summarizeByStatus();
  }
  
}
