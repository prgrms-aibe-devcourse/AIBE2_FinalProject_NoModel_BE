package com.example.nomodel.point.domain.policy;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointRewardPolicy {

    private static final BigDecimal REVIEW_REWARD = BigDecimal.valueOf(100);

    // 보상 금액 가져오기
    public BigDecimal getReviewRewardAmount() {
        return REVIEW_REWARD;
    }

}
