package com.example.nomodel.model.domain.model;

import com.example.nomodel._core.common.BaseTimeEntity;
import com.example.nomodel.model.infrastructure.AIModelEntityListener;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_model_tb")
@EntityListeners(AIModelEntityListener.class)
public class AIModel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Embedded
    private ModelMetadata modelMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "own_type", nullable = false)
    private OwnType ownType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Builder
    private AIModel(String modelName, ModelMetadata modelMetadata, OwnType ownType, 
                    Long ownerId, BigDecimal price, boolean isPublic) {
        this.modelName = modelName;
        this.modelMetadata = modelMetadata;
        this.ownType = ownType;
        this.ownerId = ownerId;
        this.price = price;
        this.isPublic = isPublic;
    }

    public static AIModel createUserModel(String modelName, ModelMetadata modelMetadata, Long ownerId) {
        return AIModel.builder()
                .modelName(modelName)
                .modelMetadata(modelMetadata)
                .ownType(OwnType.USER)
                .ownerId(ownerId)
                .price(BigDecimal.ZERO) // 유저 모델 기본 가격
                .isPublic(false) // 기본값: 비공개
                .build();
    }

    public static AIModel createAdminModel(String modelName, ModelMetadata modelMetadata, BigDecimal price) {
        return AIModel.builder()
                .modelName(modelName)
                .modelMetadata(modelMetadata)
                .ownType(OwnType.ADMIN)
                .ownerId(0L) // 관리자 소유는 0L로 표시
                .price(price)
                .isPublic(true) // 관리자 모델은 기본 공개
                .build();
    }

    public void updatePrice(BigDecimal newPrice) {
        this.price = newPrice;
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateMetadata(ModelMetadata modelMetadata) {
        this.modelMetadata = modelMetadata;
    }

    /**
     * 모델이 공개 상태인지 확인
     * @return 공개 여부
     */
    public boolean isPubliclyAvailable() {
        return this.isPublic;
    }

    /**
     * 특정 사용자가 모델의 소유자인지 확인
     * @param userId 확인할 사용자 ID
     * @return 소유자 여부
     */
    public boolean isOwnedBy(Long userId) {
        return this.ownerId.equals(userId);
    }

    /**
     * 관리자 소유 모델인지 확인
     * @return 관리자 소유 여부
     */
    public boolean isAdminModel() {
        return this.ownType.isAdminOwned();
    }

    /**
     * 유료 모델인지 확인
     * @return 유료 여부
     */
    public boolean isPaidModel() {
        return this.price != null && this.price.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 특정 사용자가 모델에 접근 가능한지 확인
     * @param userId 확인할 사용자 ID
     * @return 접근 가능 여부
     */
    public boolean isAccessibleBy(Long userId) {
        // 공개 모델이거나 소유자인 경우 접근 가능
        return this.isPublic || this.isOwnedBy(userId);
    }

    /**
     * 고해상도 이미지 생성 모델인지 확인
     * @return 고해상도 여부
     */
    public boolean isHighResolutionModel() {
        return this.modelMetadata != null && this.modelMetadata.isHighResolution();
    }
}