package com.example.nomodel.review.domain.repository;

import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ModelReview, Long> {
    // 모델별 리뷰 조회
    List<ModelReview> findByModelId(Long modelId);
    List<ModelReview> findByReviewerId(Long reviewerId);

    // 소프트 삭제 대응: ACTIVE 상태만 조회
    List<ModelReview> findByModelIdAndStatus(Long modelId, ReviewStatus status);

    // 특정 사용자가 특정 모델에 리뷰 작성했는지 여부 확인
    boolean existsByReviewerIdAndModelIdAndStatus(Long reviewerId, Long modelId, ReviewStatus status);
}

