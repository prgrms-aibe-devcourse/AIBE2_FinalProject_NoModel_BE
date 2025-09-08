package com.example.nomodel.review.application.dto.response;

import com.example.nomodel.review.domain.model.ModelReview;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    @Schema(description = "리뷰 ID", example = "1")
    private Long id;

    @Schema(description = "리뷰 작성자 ID", example = "101")
    private Long reviewerId;

    @Schema(description = "모델 ID", example = "55")
    private Long modelId;

    @Schema(description = "평점", example = "5")
    private int rating;

    @Schema(description = "리뷰 내용", example = "좋았습니다!")
    private String content;

    @Schema(description = "리뷰 상태", example = "ACTIVE")
    private String status;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;

    public static ReviewResponse from(ModelReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getReviewerId(),
                review.getModelId(),
                review.getRating().getValue(),
                review.getContent(),
                review.getStatus().name(),
                review.getCreatedAt()
        );
    }
}
