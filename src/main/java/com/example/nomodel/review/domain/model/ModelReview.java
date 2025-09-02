package com.example.nomodel.review.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_review")
public class ModelReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reviewerId;

    private Long modelId;

    @Embedded
    private Rating rating;

    private String content;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    private LocalDateTime createdAt;

    protected ModelReview() {}

    public ModelReview(Long reviewerId, Long modelId, Rating rating, String content) {
        this.reviewerId = reviewerId;
        this.modelId = modelId;
        this.rating = rating;
        this.content = content;
        this.status = ReviewStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // getter
    public Long getId() { return id; }
    public Long getReviewerId() { return reviewerId; }
    public Long getModelId() { return modelId; }
    public Rating getRating() { return rating; }
    public String getContent() { return content; }
    public ReviewStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // 비즈니스 메서드
    public void deactivate(ReviewStatus status) {
        this.status = status;
    }
}
