package com.example.nomodel.report.application.dto.response;

import com.example.nomodel.report.domain.model.Report;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminReportDto {
  Long reportId;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
  ReportStatus reportStatus;
  TargetType targetType;
  Long targetId;
  String createdBy;
  String adminNote;
  String reasonDetail;
  
  public AdminReportDto(Long reportId, LocalDateTime createdAt,
                        LocalDateTime updatedAt, ReportStatus reportStatus,
                        TargetType targetType, Long targetId, String createdBy,
                        String adminNote, String reasonDetail) {
    
    this.reportId = reportId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.reportStatus = reportStatus;
    this.targetType = targetType;
    this.targetId = targetId;
    this.createdBy = createdBy;
    this.adminNote = adminNote;
    this.reasonDetail = reasonDetail;
  }
  
  public static AdminReportDto of(Report report) {
    return AdminReportDto.builder()
            .reportId(report.getReportId())
            .createdAt(report.getCreatedAt())
            .updatedAt(report.getUpdatedAt())
            .reportStatus(report.getReportStatus())
            .targetType(report.getTargetType())
            .targetId(report.getTargetId())
            .createdBy(report.getCreatedBy())
            .adminNote(report.getAdminNote())
            .reasonDetail(report.getReasonDetail())
            .build();
  }

}
