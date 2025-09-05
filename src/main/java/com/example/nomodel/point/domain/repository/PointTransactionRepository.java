package com.example.nomodel.point.domain.repository;

import com.example.nomodel.point.domain.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByMemberId(Long memberId);
    
    // 전체 판매량
    @Query("select sum(pt.pointAmount) from PointTransaction pt")
    long sumTotalSales();
    
    // 기간 내 판매량 (예: 이번 달)
    @Query("""
           select count(ar)
             from AdResult ar
            where ar.createdAt >= :from
              and ar.createdAt <  :to
           """)
    long sumTotalSalesJoinedBetween(@Param("from") LocalDateTime from,
                                    @Param("to")   LocalDateTime to);
}
