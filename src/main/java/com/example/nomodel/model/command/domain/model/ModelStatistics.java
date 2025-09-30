package com.example.nomodel.model.command.domain.model;

import com.example.nomodel._core.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "model_statistics_tb")
public class ModelStatistics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statistics_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private AIModel model;

    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Builder
    private ModelStatistics(AIModel model) {
        this.model = model;
        // usageCount, viewCount는 필드 기본값 0L 사용
    }

    public static ModelStatistics createInitialStatistics(AIModel model) {
        return ModelStatistics.builder()
                .model(model)
                .build();
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateStatistics(long usageCount, long viewCount) {
        if (usageCount >= 0) {
            this.usageCount = usageCount;
        }
        if (viewCount >= 0) {
            this.viewCount = viewCount;
        }
    }

    /**
     * 총 상호작용 수 (사용량 + 조회수)
     * @return 총 상호작용 수
     */
    public Long getTotalInteractions() {
        return this.usageCount + this.viewCount;
    }

    /**
     * 조회수 초기화 (관리자용)
     */
    public void resetViewCount() {
        this.viewCount = 0L;
    }

    /**
     * 사용량 초기화 (관리자용)
     */
    public void resetUsageCount() {
        this.usageCount = 0L;
    }

    /**
     * 전체 통계 초기화 (관리자용)
     */
    public void resetAllStatistics() {
        this.usageCount = 0L;
        this.viewCount = 0L;
    }
}
