package com.example.nomodel.report.application.dto.request;

import com.example.nomodel.report.domain.model.ReportStatus;
import lombok.Getter;

@Getter
public class ProcessReportDto {
  private ReportStatus reportStatus;
  private String adminNote;
  
  public ProcessReportDto(ReportStatus reportStatus, String adminNote) {
    this.reportStatus = reportStatus;
    this.adminNote = adminNote;
  }
}
