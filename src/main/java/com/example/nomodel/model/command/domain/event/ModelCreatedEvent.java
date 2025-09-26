package com.example.nomodel.model.command.domain.event;

import com.example.nomodel.model.command.domain.model.AIModel;
import lombok.Getter;

/**
 * 모델 생성 이벤트
 */
@Getter
public class ModelCreatedEvent extends ModelEvent {
    private final boolean isPublic;
    private final int price;
    private final String ownType;

    public ModelCreatedEvent(AIModel model) {
        super(model.getId());
        this.isPublic = model.isPublic();
        this.price = model.getPrice().intValue();
        this.ownType = model.getOwnType().name();
    }
}