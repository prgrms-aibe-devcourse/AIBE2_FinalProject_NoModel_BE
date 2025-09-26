package com.example.nomodel.model.query.service;

import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.command.application.dto.response.AIModelStaticDetail;
import com.example.nomodel.model.command.domain.service.ModelViewCountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐시가 적용된 AI 모델 상세 조회 서비스
 * 조회수 증가와 캐시를 분리하여 처리
 */
@Slf4j
@Service
public class CachedModelDetailService {

    private final AIModelDetailService modelDetailService;
    private final ModelViewCountService viewCountService;
    private final CachedModelDetailService self; // AOP 적용된 프록시

    public CachedModelDetailService(AIModelDetailService modelDetailService,
                                   ModelViewCountService viewCountService,
                                   @Lazy CachedModelDetailService self) {
        this.modelDetailService = modelDetailService;
        this.viewCountService = viewCountService;
        this.self = self;
    }

    public AIModelStaticDetail getModelStaticDetailWithView(Long modelId, Long memberId) {
        viewCountService.processViewCountAsync(modelId, memberId);
        return self.getModelStaticDetail(modelId);
    }

    @Cacheable(value = "modelDetail", key = "#modelId", unless = "#result == null")
    @Transactional(readOnly = true)
    public AIModelStaticDetail getModelStaticDetail(Long modelId) {
        log.debug("캐시 미스 - 모델 정적 상세 조회 실행: modelId={}", modelId);
        return modelDetailService.getModelStaticDetail(modelId);
    }
}