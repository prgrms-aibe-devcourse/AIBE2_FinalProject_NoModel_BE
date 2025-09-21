package com.example.nomodel.model.command.domain.repository;

import com.example.nomodel.model.command.domain.model.ModelStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ModelStatisticsJpaRepository extends JpaRepository<ModelStatistics, Long> {

    /**
     * 특정 모델의 통계 조회
     */
    Optional<ModelStatistics> findByModelId(Long modelId);

    /**
     * 특정 모델의 통계 존재 여부 확인
     */
    boolean existsByModelId(Long modelId);

    /**
     * 모델별 사용 수 상위 N개 조회
     */
    @Query("SELECT ms FROM ModelStatistics ms ORDER BY ms.usageCount DESC")
    List<ModelStatistics> findTopByOrderByUsageCountDesc();

    /**
     * 모델별 조회 수 상위 N개 조회
     */
    @Query("SELECT ms FROM ModelStatistics ms ORDER BY ms.viewCount DESC")
    List<ModelStatistics> findTopByOrderByViewCountDesc();

    /**
     * 특정 기간 동안 업데이트된 통계 조회
     */
    List<ModelStatistics> findByUpdatedAtAfter(LocalDateTime updatedAt);

    /**
     * 최소 사용 수 이상인 통계 조회
     */
    List<ModelStatistics> findByUsageCountGreaterThanEqual(Long minUsageCount);

    /**
     * 최소 조회 수 이상인 통계 조회
     */
    List<ModelStatistics> findByViewCountGreaterThanEqual(Long minViewCount);

    /**
     * 인기 모델 통계 조회 (복합 기준)
     */
    @Query("SELECT ms FROM ModelStatistics ms WHERE ms.usageCount >= :minUsage AND ms.viewCount >= :minView ORDER BY ms.usageCount DESC, ms.viewCount DESC")
    List<ModelStatistics> findPopularModels(@Param("minUsage") Long minUsage, 
                                           @Param("minView") Long minView);

    /**
     * 활성 모델 통계 조회 (최근 일정 기간 내 업데이트)
     */
    @Query("SELECT ms FROM ModelStatistics ms WHERE ms.updatedAt >= :since ORDER BY ms.updatedAt DESC")
    List<ModelStatistics> findActiveStatistics(@Param("since") LocalDateTime since);

    /**
     * 전체 사용 수 합계 조회
     */
    @Query("SELECT SUM(ms.usageCount) FROM ModelStatistics ms")
    Long getTotalUsageCount();

    /**
     * 전체 조회 수 합계 조회
     */
    @Query("SELECT SUM(ms.viewCount) FROM ModelStatistics ms")
    Long getTotalViewCount();
}