package com.example.nomodel.point.domain.repository;

import com.example.nomodel.point.domain.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByMemberId(Long memberId);
}
