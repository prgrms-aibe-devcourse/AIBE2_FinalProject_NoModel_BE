package com.example.nomodel.point.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "review_reward_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_reward_member_model",
                columnNames = {"member_id", "model_id"}
        )
)
public class ReviewRewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private Long modelId;
    private LocalDateTime createdAt;

    protected ReviewRewardTransaction() {}

    public ReviewRewardTransaction(Long memberId, Long modelId) {
        this.memberId = memberId;
        this.modelId = modelId;
        this.createdAt = LocalDateTime.now();
    }

    // getter
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getModelId() { return modelId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
