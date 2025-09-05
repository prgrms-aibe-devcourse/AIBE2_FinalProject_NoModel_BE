package com.example.nomodel.review.domain.repository;

import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ModelReview, Long> {
    // 모델별 리뷰 조회
    List<ModelReview> findByModelId(Long modelId);
    List<ModelReview> findByReviewerId(Long reviewerId);

    // 소프트 삭제 대응: ACTIVE 상태만 조회
    List<ModelReview> findByModelIdAndStatus(Long modelId, ReviewStatus status);

    // 특정 사용자가 특정 모델에 리뷰 작성했는지 여부 확인
    boolean existsByReviewerIdAndModelIdAndStatus(Long reviewerId, Long modelId, ReviewStatus status);

    /**
     * 모델의 활성 리뷰 개수 조회
     */
    long countByModelIdAndStatus(Long modelId, ReviewStatus status);

    /**
     * 모델의 평점 평균 계산
     */
    @Query("SELECT AVG(r.rating.value) FROM ModelReview r WHERE r.modelId = :modelId AND r.status = :status")
    Double calculateAverageRatingByModelId(@Param("modelId") Long modelId, @Param("status") ReviewStatus status);

    /**
     * 여러 모델의 리뷰 통계를 일괄 조회 (N+1 방지)
     */
    @Query("SELECT r.modelId, COUNT(r), AVG(r.rating.value) FROM ModelReview r WHERE r.modelId IN :modelIds AND r.status = :status GROUP BY r.modelId")
    List<Object[]> getReviewStatisticsByModelIds(@Param("modelIds") List<Long> modelIds, @Param("status") ReviewStatus status);
}

