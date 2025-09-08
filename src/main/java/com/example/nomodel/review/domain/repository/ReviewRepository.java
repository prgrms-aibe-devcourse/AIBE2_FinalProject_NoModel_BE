package com.example.nomodel.review.domain.repository;

import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.statistics.application.dto.response.RatingSummaryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ModelReview, Long> {
    List<ModelReview> findByModelId(Long modelId);
    List<ModelReview> findByReviewerId(Long reviewerId);
    
    @Query("select avg(mv.rating.value) from ModelReview mv")
    long averageRating();

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
}
