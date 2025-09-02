package com.example.nomodel.review.domain.repository;

import com.example.nomodel.review.domain.model.ModelReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ModelReview, Long> {
    //모델별 리뷰 조회
    List<ModelReview> findByModelId(Long modelId);
    List<ModelReview> findByReviewerId(Long reviewerId);

    // 특정 사용자가 특정 모델에 리뷰를 작성했는지 여부 확인
    boolean existsByReviewerIdAndModelId(Long reviewerId, Long modelId);
}
