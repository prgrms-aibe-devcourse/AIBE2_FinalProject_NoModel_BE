package com.example.nomodel.report.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Status;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.report.application.dto.request.ProcessReportDto;
import com.example.nomodel.report.application.dto.response.AdminReportDto;
import com.example.nomodel.report.application.dto.response.AdminReportSummaryDto;
import com.example.nomodel.report.domain.model.Report;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import com.example.nomodel.report.domain.repository.ReportJpaRepository;
import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminReportService {
  private final ReportJpaRepository reportJpaRepository;
  private final ReviewRepository reviewRepository;
  private final MemberJpaRepository memberJpaRepository;
  private final AIModelJpaRepository aiModelJpaRepository;
  
  public AdminReportSummaryDto getReportSummary() {
    return reportJpaRepository.summarizeByStatus();
  }
  
  public Page<AdminReportDto> getReport(int page, int size,
                                        TargetType targetType,
                                        ReportStatus reportStatus) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return reportJpaRepository.findAdminReportPage(targetType, reportStatus, pageable);
  }
  
  public AdminReportDto getReportDetail(String reportId) {
    Report report = reportJpaRepository.findById(Long.valueOf(reportId))
            .orElseThrow(() -> new ApplicationException(ErrorCode.REPORT_NOT_FOUND));
    return AdminReportDto.of(report);
  }
  
  @Transactional
  public AdminReportDto processReport(Long reportId, ProcessReportDto req) {
    Report report = reportJpaRepository.findById(reportId)
            .orElseThrow(() -> new ApplicationException(ErrorCode.REPORT_NOT_FOUND));
    
    validateRequest(report, req);
    
    // adminNote 저장 (ACCEPTED/UNDER_REVIEW/REJECTED/RESOLVED 모두 저장)
    report.setAdminNote(req.getAdminNote());
    report.setReportStatus(req.getReportStatus());
    report.setUpdatedAt(LocalDateTime.now());
    
    if (req.getReportStatus() == ReportStatus.RESOLVED) {
      switch (report.getTargetType()) {
        case REVIEW -> suspendUserByReport(report, req);
        case MODEL -> suspendModelByReport(report, req);
      }
    }
    
    Report saved = reportJpaRepository.save(report);
    return AdminReportDto.of(saved);
  }
  
  private void validateRequest(Report report, ProcessReportDto req) {
    if (req.getReportStatus() == null) {
      throw new ApplicationException(ErrorCode.INVALID_REQUEST);
    }
  }
  
  private void suspendUserByReport(Report report, ProcessReportDto req) {
    if (report.getTargetType() != TargetType.REVIEW) {
      throw new ApplicationException(ErrorCode.INVALID_REQUEST);
    }
    
    ModelReview modelReview = reviewRepository.findById(report.getTargetId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST));
    // review 작성자 사용 정지
    Member member = memberJpaRepository.findById(modelReview.getReviewerId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));
    member.setStatus(Status.BANNED);
  }
  
  private void suspendModelByReport(Report report, ProcessReportDto req) {
    if (report.getTargetType() != TargetType.MODEL) {
      throw new ApplicationException(ErrorCode.INVALID_REQUEST);
    }
    // ai model 사용 정지
    AIModel aiModel = aiModelJpaRepository.findById(report.getTargetId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST));
    aiModel.setPublic(false);
    // ai model 오너 사용 정지
    Member member = memberJpaRepository.findById(aiModel.getOwnerId())
            .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));
    member.setStatus(Status.BANNED);
    
  }
}
