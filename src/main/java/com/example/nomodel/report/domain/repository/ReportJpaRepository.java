package com.example.nomodel.report.domain.repository;

import com.example.nomodel.report.domain.model.Report;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<Report, Long> {

    /**
     * 특정 대상에 대한 신고 목록 조회
     */
    List<Report> findByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    /**
     * 신고 상태별 조회
     */
    List<Report> findByReportStatus(ReportStatus reportStatus);

    /**
     * 특정 신고자의 신고 목록 조회
     */
    @Query("SELECT r FROM Report r WHERE r.createdBy = :reporterId")
    List<Report> findByReporterId(@Param("reporterId") String reporterId);

    /**
     * 대상 타입별 신고 목록 조회
     */
    List<Report> findByTargetType(TargetType targetType);

    /**
     * 대기 중인 신고 목록 조회
     */
    List<Report> findByReportStatusOrderByCreatedAtAsc(ReportStatus reportStatus);

    /**
     * 특정 기간 내 신고 목록 조회
     */
    List<Report> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 사용자가 특정 대상에 신고한 이력 조회
     */
    @Query("SELECT r FROM Report r WHERE r.createdBy = :reporterId AND r.targetType = :targetType AND r.targetId = :targetId")
    List<Report> findByReporterAndTarget(@Param("reporterId") String reporterId, 
                                        @Param("targetType") TargetType targetType, 
                                        @Param("targetId") Long targetId);

    /**
     * 특정 사용자가 특정 대상에 이미 신고했는지 확인
     */
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.createdBy = :reporterId AND r.targetType = :targetType AND r.targetId = :targetId AND r.reportStatus != :excludeStatus")
    boolean existsByReporterAndTargetExcludingStatus(@Param("reporterId") String reporterId, 
                                                    @Param("targetType") TargetType targetType, 
                                                    @Param("targetId") Long targetId,
                                                    @Param("excludeStatus") ReportStatus excludeStatus);

    /**
     * 특정 대상의 신고 수 카운트
     */
    long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

    /**
     * 특정 대상의 활성 신고 수 카운트 (해결되지 않은 신고)
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = :targetType AND r.targetId = :targetId AND r.reportStatus IN :activeStatuses")
    long countActiveReportsByTarget(@Param("targetType") TargetType targetType, 
                                   @Param("targetId") Long targetId,
                                   @Param("activeStatuses") List<ReportStatus> activeStatuses);

    /**
     * 상태별 신고 수 카운트
     */
    long countByReportStatus(ReportStatus reportStatus);

    /**
     * 최근 신고 목록 조회
     */
    List<Report> findTop10ByOrderByCreatedAtDesc();

    /**
     * 처리가 필요한 신고 목록 조회 (PENDING, UNDER_REVIEW)
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus IN ('PENDING', 'UNDER_REVIEW') ORDER BY r.createdAt ASC")
    List<Report> findReportsNeedingAttention();

    /**
     * 특정 관리자가 처리한 신고 목록 조회
     */
    @Query("SELECT r FROM Report r WHERE r.modifiedBy = :adminId AND r.reportStatus IN ('RESOLVED', 'REJECTED')")
    List<Report> findReportsProcessedByAdmin(@Param("adminId") String adminId);

    /**
     * 오래된 대기 중인 신고 목록 조회
     */
    @Query("SELECT r FROM Report r WHERE r.reportStatus = 'PENDING' AND r.createdAt < :threshold ORDER BY r.createdAt ASC")
    List<Report> findOldPendingReports(@Param("threshold") LocalDateTime threshold);
}