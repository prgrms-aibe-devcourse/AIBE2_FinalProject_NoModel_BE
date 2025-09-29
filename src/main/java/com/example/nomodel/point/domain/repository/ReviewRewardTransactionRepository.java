package com.example.nomodel.point.domain.repository;

import com.example.nomodel.point.domain.model.ReviewRewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRewardTransactionRepository extends JpaRepository<ReviewRewardTransaction, Long> {
    
    boolean existsByMemberIdAndModelId(Long memberId, Long modelId);
}
