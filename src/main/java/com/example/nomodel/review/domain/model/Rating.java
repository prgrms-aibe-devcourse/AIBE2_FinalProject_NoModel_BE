package com.example.nomodel.review.domain.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Rating {

    private Integer value;

    protected Rating() {}

    public Rating(Integer value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
