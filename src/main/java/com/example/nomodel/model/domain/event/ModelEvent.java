package com.example.nomodel.model.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모델 관련 이벤트 기본 클래스
 */
@Getter
@RequiredArgsConstructor
public abstract class ModelEvent {
    private final Long modelId;
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final String eventType = this.getClass().getSimpleName();
}