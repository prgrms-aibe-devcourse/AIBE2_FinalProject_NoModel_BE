package com.example.nomodel.point.domain.repository;

import com.example.nomodel.point.domain.model.PointTransaction;
import com.example.nomodel.point.domain.model.RefererType;
import com.example.nomodel.point.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.nomodel.statistics.application.dto.response.MonthlyCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByMemberId(Long memberId);
    boolean existsByMemberIdAndRefererTypeAndRefererIdAndTransactionType(
            Long memberId,
            RefererType refererType,
            Long refererId,
            TransactionType transactionType
    );

    Page<PointTransaction> findByMemberId(Long memberId, Pageable pageable);

    List<PointTransaction> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    
    // 전체 판매량
    @Query("select sum(pt.pointAmount) from PointTransaction pt")
    long sumTotalSales();
    
    // 기간 내 판매량 (예: 이번 달)
    @Query("""
           select sum(pt.pointAmount)
             from PointTransaction pt
            where pt.createdAt >= :from
              and pt.createdAt <  :to
           """)
    long sumTotalSalesJoinedBetween(@Param("from") LocalDateTime from,
                                    @Param("to")   LocalDateTime to);
    
    // 월간 판매량
    @Query("""
           select new com.example.nomodel.statistics.application.dto.response.MonthlyCount(
                    cast(year(pt.createdAt) as integer), cast(month(pt.createdAt) as integer), cast(sum(pt.pointAmount) as long))
             from PointTransaction pt
            where (:from is null or pt.createdAt >= :from)
              and (:to   is null or pt.createdAt <  :to)
         group by cast(year(pt.createdAt) as integer), cast(month(pt.createdAt) as integer)
         order by cast(year(pt.createdAt) as integer), cast(month(pt.createdAt) as integer)
           """)
    List<MonthlyCount> sumRevenueByMonth(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );
}
