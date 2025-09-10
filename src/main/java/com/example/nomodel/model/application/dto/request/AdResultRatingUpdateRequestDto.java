package com.example.nomodel.model.application.dto.request;

import jakarta.validation.constraints.*;

public record AdResultRatingUpdateRequestDto(
    
    @NotNull(message = "평점은 필수입니다")
    @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다")
    Double memberRating
) {
}