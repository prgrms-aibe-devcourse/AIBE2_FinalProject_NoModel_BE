package com.example.nomodel.model.command.domain.event;

/**
 * 모델 삭제 이벤트
 */
public class ModelDeletedEvent extends ModelEvent {

    public ModelDeletedEvent(Long modelId) {
        super(modelId);
    }
}