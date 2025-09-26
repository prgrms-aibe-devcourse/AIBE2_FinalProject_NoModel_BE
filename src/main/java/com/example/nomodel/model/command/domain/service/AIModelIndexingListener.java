package com.example.nomodel.model.command.domain.service;

import com.example.nomodel.model.command.domain.event.ModelCreatedEvent;
import com.example.nomodel.model.command.domain.event.ModelDeletedEvent;
import com.example.nomodel.model.command.domain.event.ModelUpdateEvent;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.command.infrastructure.service.ElasticsearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelIndexingListener {

    private final ElasticsearchIndexService indexService;
    private final AIModelJpaRepository aiModelRepository;

    /**
     * 모델 생성 시 색인
     */
    @EventListener
    @Async
    public void onModelCreated(ModelCreatedEvent event) {
        log.info("ModelCreatedEvent 수신 - modelId={}", event.getModelId());
        aiModelRepository.findById(event.getModelId()).ifPresent(indexService::indexModel);
    }

    /**
     * 모델 수정 시 재색인
     */
    @EventListener
    @Async
    public void onModelUpdated(ModelUpdateEvent event) {
        log.info("ModelUpdateEvent 수신 - modelId={}", event.getModelId());
        aiModelRepository.findById(event.getModelId()).ifPresent(indexService::indexModel);
    }

    /**
     * 모델 삭제 시 인덱스 제거
     */
    @EventListener
    @Async
    public void onModelDeleted(ModelDeletedEvent event) {
        log.info("ModelDeletedEvent 수신 - modelId={}", event.getModelId());
        indexService.deleteModelIndex(event.getModelId());
    }
}
