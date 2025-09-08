package com.example.nomodel.review.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequest {

    @Schema(description = "리뷰 평점 (1~5)", example = "5")
    private int rating;

    @Schema(description = "리뷰 내용", example = "모델 결과물이 정말 마음에 들어요!")
    private String content;
}
