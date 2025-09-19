package com.example.nomodel.review.domain.repository;

import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.statistics.application.dto.response.RatingSummaryDto;
import com.example.nomodel.review.domain.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ModelReview, Long> {
    // 모델별 리뷰 조회
    List<ModelReview> findByModelId(Long modelId);
    List<ModelReview> findByReviewerId(Long reviewerId);

    // 평점 평균 조회
    @Query("select avg(mv.rating.value) from ModelReview mv")
    long averageRating();

    // 평점 분포 조회
    @Query("""
        select new com.example.nomodel.statistics.application.dto.response.RatingSummaryDto(
            sum(case when r.rating.value = 1 then 1 else 0 end),
            sum(case when r.rating.value = 2 then 1 else 0 end),
            sum(case when r.rating.value = 3 then 1 else 0 end),
            sum(case when r.rating.value = 4 then 1 else 0 end),
            sum(case when r.rating.value = 5 then 1 else 0 end),
            
            count(r)
        )
        from ModelReview r
        """)
    RatingSummaryDto getRatingSummary();

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

    // ReviewRepository 인터페이스에 추가할 메서드

    /**
     * 특정 사용자가 작성한 특정 상태의 리뷰들을 최신순으로 조회
     */
    List<ModelReview> findByReviewerIdAndStatusOrderByCreatedAtDesc(Long reviewerId, ReviewStatus status);
    /**
     * 특정 리뷰 ID와 작성자 ID로 조회 (권한 체크용)
     */
    Optional<ModelReview> findByIdAndReviewerId(Long reviewId, Long reviewerId);

    /**
     * 특정 사용자가 특정 모델에 작성한 특정 상태의 리뷰 조회
     */
    Optional<ModelReview> findByReviewerIdAndModelIdAndStatus(Long reviewerId, Long modelId, ReviewStatus status);
}

