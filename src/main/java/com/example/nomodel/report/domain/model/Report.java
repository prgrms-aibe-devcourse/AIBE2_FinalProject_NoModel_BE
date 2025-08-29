package com.example.nomodel.report.domain.model;

import com.example.nomodel._core.common.BaseEntity;
import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "report_tb")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;


    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "reason_detail", length = 1000)
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false)
    private ReportStatus reportStatus;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Builder
    private Report(TargetType targetType, Long targetId, 
                   String reasonDetail, ReportStatus reportStatus, String adminNote) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonDetail = reasonDetail;
        this.reportStatus = reportStatus;
        this.adminNote = adminNote;
    }

    public static Report createReport(TargetType targetType, Long targetId, String reasonDetail) {
        return Report.builder()
                .targetType(targetType)
                .targetId(targetId)
                .reasonDetail(reasonDetail)
                .reportStatus(ReportStatus.PENDING)
                .build();
    }

    public void startReview() {
        if (this.reportStatus != ReportStatus.PENDING) {
            throw new ApplicationException(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
        }
        this.reportStatus = ReportStatus.UNDER_REVIEW;
    }

    public void accept(String adminNote) {
        if (this.reportStatus != ReportStatus.UNDER_REVIEW) {
            throw new ApplicationException(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
        }
        this.reportStatus = ReportStatus.ACCEPTED;
        this.adminNote = adminNote;
    }

    public void reject(String adminNote) {
        if (this.reportStatus != ReportStatus.UNDER_REVIEW) {
            throw new ApplicationException(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
        }
        this.reportStatus = ReportStatus.REJECTED;
        this.adminNote = adminNote;
    }

    public void resolve(String adminNote) {
        if (this.reportStatus != ReportStatus.ACCEPTED) {
            throw new ApplicationException(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
        }
        this.reportStatus = ReportStatus.RESOLVED;
        this.adminNote = adminNote;
    }

    /**
     * 신고자인지 확인
     * @param memberId 확인할 회원 ID
     * @return 신고자 여부
     */
    public boolean isReportedBy(Long memberId) {
        if (memberId == null) {
            return false;
        }
        return getCreatedBy() != null && getCreatedBy().equals(String.valueOf(memberId));
    }

    /**
     * 신고자 ID 조회 (Long 타입)
     * @return 신고자 ID
     */
    public Long getReporterId() {
        if (getCreatedBy() == null) {
            throw new ApplicationException(ErrorCode.REPORT_INVALID_REPORTER);
        }
        return Long.valueOf(getCreatedBy());
    }

    /**
     * 특정 대상에 대한 신고인지 확인
     * @param targetType 대상 타입
     * @param targetId 대상 ID
     * @return 해당 대상 신고 여부
     */
    public boolean isReportFor(TargetType targetType, Long targetId) {
        return this.targetType == targetType && this.targetId.equals(targetId);
    }

    /**
     * 처리 완료된 신고인지 확인
     * @return 완료 여부
     */
    public boolean isCompleted() {
        return this.reportStatus.isCompleted();
    }

    /**
     * 관리자 처리가 필요한 신고인지 확인
     * @return 처리 필요 여부
     */
    public boolean requiresAdminAction() {
        return this.reportStatus == ReportStatus.PENDING || 
               this.reportStatus == ReportStatus.UNDER_REVIEW;
    }
}