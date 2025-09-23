package com.example.nomodel.review.domain.model;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Rating {
    // VALUE는 H2, MySQL 모두에서 예약어(reserved keyword)라서 컬럼명으로 쓰면 SQL 문법 오류
    @Column(name = "rating_value", nullable = false)
    private Integer value;

    protected Rating() {}

    public Rating(Integer value) {
        if (value == null) {
            throw new ApplicationException(ErrorCode.INVALID_RATING_VALUE);
        }
        if (value < 1 || value > 5) {
            throw new ApplicationException(ErrorCode.INVALID_RATING_VALUE);
        }
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
