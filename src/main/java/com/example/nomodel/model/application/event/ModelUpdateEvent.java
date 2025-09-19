package com.example.nomodel.model.application.event;

import lombok.Getter;

/**
 * 모델 업데이트 이벤트
 */
@Getter
public class ModelUpdateEvent extends ModelEvent {
    private final String updateType;  // BASIC_INFO, PRICE, VISIBILITY, FILES
    private final Object oldValue;
    private final Object newValue;

    public ModelUpdateEvent(Long modelId, String updateType, Object oldValue, Object newValue) {
        super(modelId);
        this.updateType = updateType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public static ModelUpdateEvent priceChange(Long modelId, Object oldPrice, Object newPrice) {
        return new ModelUpdateEvent(modelId, "PRICE", oldPrice, newPrice);
    }

    public static ModelUpdateEvent visibilityChange(Long modelId, Boolean oldVisibility, Boolean newVisibility) {
        return new ModelUpdateEvent(modelId, "VISIBILITY", oldVisibility, newVisibility);
    }

    public static ModelUpdateEvent basicInfoChange(Long modelId) {
        return new ModelUpdateEvent(modelId, "BASIC_INFO", null, null);
    }

    public static ModelUpdateEvent filesChange(Long modelId) {
        return new ModelUpdateEvent(modelId, "FILES", null, null);
    }
}