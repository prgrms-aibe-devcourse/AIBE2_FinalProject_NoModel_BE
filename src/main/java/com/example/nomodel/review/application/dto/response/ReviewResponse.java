package com.example.nomodel.review.application.dto.response;

import java.time.LocalDateTime;

public class ReviewResponse {
    private Long id;
    private Long reviewerId;
    private Long modelId;
    private Integer rating;
    private String content;
    private String status;
    private LocalDateTime createdAt;

    public ReviewResponse(Long id, Long reviewerId, Long modelId, Integer rating,
                          String content, String status, LocalDateTime createdAt) {
        this.id = id;
        this.reviewerId = reviewerId;
        this.modelId = modelId;
        this.rating = rating;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    // getter
    public Long getId() { return id; }
    public Long getReviewerId() { return reviewerId; }
    public Long getModelId() { return modelId; }
    public Integer getRating() { return rating; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
