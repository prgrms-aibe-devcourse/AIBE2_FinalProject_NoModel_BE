package com.example.nomodel.report.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.report.application.dto.request.ModelReportRequest;
import com.example.nomodel.report.application.dto.response.ModelReportResponse;
import com.example.nomodel.report.domain.model.Report;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import com.example.nomodel.report.domain.repository.ReportJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 모델 신고 서비스
 * AI 모델에 대한 신고 생성, 조회 및 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelReportService {

    private final ReportJpaRepository reportRepository;
    private final AIModelJpaRepository aiModelRepository;

    /**
     * 모델 신고 생성
     * 
     * @param modelId 신고할 모델 ID
     * @param reporterId 신고자 ID
     * @param request 신고 요청 정보
     * @return 생성된 신고 정보
     */
    @Transactional
    public ModelReportResponse createModelReport(Long modelId, Long reporterId, ModelReportRequest request) {
        // 1. 모델 존재 확인
        if (!aiModelRepository.existsById(modelId)) {
            throw new ApplicationException(ErrorCode.MODEL_NOT_FOUND);
        }

        // 2. 중복 신고 확인 (해결된 신고 제외)
        if (hasAlreadyReported(reporterId, modelId)) {
            throw new ApplicationException(ErrorCode.DUPLICATE_REPORT);
        }

        // 3. 신고 생성
        Report report = Report.createReport(
            TargetType.MODEL, 
            modelId, 
            request.getReasonDetail()
        );

        // BaseEntity의 createdBy 필드는 @CreatedBy로 자동 설정되지만, 
        // 명시적으로 신고자 정보를 설정해야 할 수도 있음
        Report savedReport = reportRepository.save(report);

        log.info("모델 신고 생성 완료: modelId={}, reporterId={}, reportId={}", 
                modelId, reporterId, savedReport.getReportId());

        return ModelReportResponse.fromModelReport(savedReport);
    }

    /**
     * 특정 모델에 대한 신고 목록 조회
     * 
     * @param modelId 모델 ID
     * @return 해당 모델의 신고 목록
     */
    public List<ModelReportResponse> getModelReports(Long modelId) {
        List<Report> reports = reportRepository.findByTargetTypeAndTargetId(TargetType.MODEL, modelId);
        
        return reports.stream()
                .map(ModelReportResponse::fromModelReport)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 모델 신고 목록 조회
     * 
     * @param reporterId 신고자 ID
     * @return 사용자의 모든 모델 신고 목록
     */
    public List<ModelReportResponse> getUserModelReports(Long reporterId) {
        List<Report> reports = reportRepository.findByReporterId(String.valueOf(reporterId));
        
        return reports.stream()
                .filter(report -> report.getTargetType().isModelReport())
                .map(ModelReportResponse::fromModelReport)
                .collect(Collectors.toList());
    }

    /**
     * 특정 신고 조회
     * 
     * @param reportId 신고 ID
     * @param reporterId 신고자 ID (권한 확인용)
     * @return 신고 정보
     */
    public ModelReportResponse getModelReport(Long reportId, Long reporterId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REPORT_NOT_FOUND));

        // 모델 신고가 아닌 경우
        if (!report.getTargetType().isModelReport()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        // 신고자 본인만 조회 가능
        if (!report.isReportedBy(reporterId)) {
            throw new ApplicationException(ErrorCode.REPORT_ACCESS_DENIED);
        }

        return ModelReportResponse.fromModelReport(report);
    }

    /**
     * 모델의 활성 신고 수 조회
     * 
     * @param modelId 모델 ID
     * @return 활성 신고 수 (PENDING, UNDER_REVIEW 상태)
     */
    public long getActiveReportCount(Long modelId) {
        List<ReportStatus> activeStatuses = List.of(ReportStatus.PENDING, ReportStatus.UNDER_REVIEW);
        return reportRepository.countActiveReportsByTarget(TargetType.MODEL, modelId, activeStatuses);
    }

    /**
     * 모델의 전체 신고 수 조회
     * 
     * @param modelId 모델 ID
     * @return 전체 신고 수
     */
    public long getTotalReportCount(Long modelId) {
        return reportRepository.countByTargetTypeAndTargetId(TargetType.MODEL, modelId);
    }

    /**
     * 중복 신고 확인
     * 
     * @param reporterId 신고자 ID
     * @param modelId 모델 ID
     * @return 이미 신고했는지 여부
     */
    private boolean hasAlreadyReported(Long reporterId, Long modelId) {
        // REJECTED 상태는 제외하고 중복 확인 (재신고 허용)
        return reportRepository.existsByReporterAndTargetExcludingStatus(
            String.valueOf(reporterId), 
            TargetType.MODEL, 
            modelId, 
            ReportStatus.REJECTED
        );
    }
}