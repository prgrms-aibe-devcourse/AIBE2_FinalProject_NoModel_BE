package com.example.nomodel.review.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Rating {

    // VALUE는 H2, MySQL 모두에서 예약어(reserved keyword)라서 컬럼명으로 쓰면 SQL 문법 오류
    @Column(name = "rating_value", nullable = false)
    private Integer value;

    protected Rating() {
    }

    public Rating(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("별점은 null일 수 없습니다.");
        }
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
