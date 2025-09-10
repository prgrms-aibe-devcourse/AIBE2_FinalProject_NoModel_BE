package com.example.nomodel.model.application.dto.response;

public record AdResultAverageRatingResponseDto(
    Double averageRating
) {
    
    public static AdResultAverageRatingResponseDto from(Double averageRating) {
        return new AdResultAverageRatingResponseDto(
            averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : null
        );
    }
}