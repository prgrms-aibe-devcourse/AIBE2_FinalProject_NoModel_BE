package com.example.nomodel.review.domain.repository;

import com.example.nomodel.review.domain.model.ModelReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ModelReview, Long> {
    List<ModelReview> findByModelId(Long modelId);
    List<ModelReview> findByReviewerId(Long reviewerId);
}
