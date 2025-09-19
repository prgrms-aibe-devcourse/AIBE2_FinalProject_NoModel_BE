package com.example.nomodel.model.domain.event;

import lombok.Getter;

/**
 * 모델 생성 이벤트
 */
@Getter
public class ModelCreatedEvent extends ModelEvent {
    private final boolean isPublic;
    private final boolean isFree;
    private final String ownType;

    public ModelCreatedEvent(Long modelId, boolean isPublic, boolean isFree, String ownType) {
        super(modelId);
        this.isPublic = isPublic;
        this.isFree = isFree;
        this.ownType = ownType;
    }
}