package com.example.nomodel.model.command.domain.service;

import com.example.nomodel.model.command.domain.event.ModelCreatedEvent;
import com.example.nomodel.model.command.domain.event.ModelDeletedEvent;
import com.example.nomodel.model.command.domain.event.ModelUpdateEvent;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.command.infrastructure.service.ElasticsearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
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
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModelCreated(ModelCreatedEvent event) {
        log.info("ModelCreatedEvent 수신 - modelId={}", event.getModelId());
        aiModelRepository.findById(event.getModelId()).ifPresent(indexService::indexModel);
    }

    /**
     * 모델 수정 시 재색인
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModelUpdated(ModelUpdateEvent event) {
        log.info("ModelUpdateEvent 수신 - modelId={}", event.getModelId());
        aiModelRepository.findById(event.getModelId()).ifPresent(indexService::indexModel);
    }

    /**
     * 모델 삭제 시 인덱스 제거
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModelDeleted(ModelDeletedEvent event) {
        log.info("ModelDeletedEvent 수신 - modelId={}", event.getModelId());
        indexService.deleteModelIndex(event.getModelId());
    }
}
