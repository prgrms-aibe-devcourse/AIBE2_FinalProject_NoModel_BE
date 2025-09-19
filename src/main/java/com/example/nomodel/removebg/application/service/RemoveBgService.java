package com.example.nomodel.removebg.application.service;

import com.example.nomodel.generationjob.application.service.GenerationJobService;
import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RemoveBgService {

    private final GenerationJobService jobs;
    private final FileService fileService;
    private final ObjectMapper om;
    private final ApplicationEventPublisher eventPublisher;
    private final WebClient removeBgWebClient;
    
    @Value("${BG_PROVIDER:dummy}")
    private String bgProvider;

    public RemoveBgService(GenerationJobService jobs,
                          FileService fileService,
                          ObjectMapper om,
                          ApplicationEventPublisher eventPublisher,
                          @Autowired(required = false) @Qualifier("removeBgWebClient") WebClient removeBgWebClient) {
        this.jobs = jobs;
        this.fileService = fileService;
        this.om = om;
        this.eventPublisher = eventPublisher;
        this.removeBgWebClient = removeBgWebClient; // null일 수 있음
    }

    @Transactional
    public UUID enqueue(Long ownerId, Long fileId) {
        Map<String, Object> params = Map.of("retry", 2);
        String paramsJson = write(params);

        var job = jobs.enqueueRemoveBg(ownerId, fileId, paramsJson);
        
        log.info("[RemoveBgService] Job enqueued: {}, provider: {}", job.getId(), bgProvider);

        eventPublisher.publishEvent(new RemoveBgJobEvent(job.getId(), params));

        return job.getId();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRemoveBgJobEvent(RemoveBgJobEvent event) {
        log.info("[RemoveBgService] Handling job event for: {}", event.jobId());
        try {
            executeRemoveBgAsync(event.jobId(), event.params());
        } catch (Exception e) {
            log.error("[RemoveBgService] Failed to start async job execution for: {}", event.jobId(), e);
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeRemoveBgAsync(UUID jobId, Map<String, Object> params) {
        log.info("[RemoveBgService] Starting async execution for job: {}", jobId);
        
        jobs.runRemoveBg(jobId, (fileId, opts) -> {
            try {
                return removeBackground(fileId, opts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, params);
    }

    /**
     * 배경 제거 실행 - provider에 따라 다른 방식 사용
     */
    private Long removeBackground(Long originalFileId, Map<String, Object> opts) throws Exception {
        log.info("[RemoveBgService] Processing fileId: {} with provider: {}", originalFileId, bgProvider);
        
        return switch (bgProvider) {
            case "removebg" -> removeBackgroundWithApi(originalFileId, opts);
            default -> removeBackgroundDummy(originalFileId, opts);
        };
    }

    /**
     * remove.bg API를 사용한 배경 제거
     */
    private Long removeBackgroundWithApi(Long originalFileId, Map<String, Object> opts) throws Exception {
        if (removeBgWebClient == null) {
            log.warn("[RemoveBgService] RemoveBg API configured but WebClient not available, falling back to dummy mode");
            return removeBackgroundDummy(originalFileId, opts);
        }

        log.info("[RemoveBgService] Using remove.bg API for fileId: {}", originalFileId);
        
        byte[] original = fileService.loadAsBytes(originalFileId);

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("image_file", original)
                .filename("input.png")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        String size = String.valueOf(opts.getOrDefault("size", "auto"));
        String format = String.valueOf(opts.getOrDefault("format", "png"));
        mb.part("size", size);
        mb.part("format", format);

        int retry = Integer.parseInt(String.valueOf(opts.getOrDefault("retry", 1)));
        long timeoutSec = Long.parseLong(String.valueOf(opts.getOrDefault("timeoutSec", 60)));

        log.info("[remove.bg] API call: fileId={}, size={}, format={}", originalFileId, size, format);

        byte[] result = removeBgWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1.0/removebg").build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), r -> r.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("remove.bg error: " + body))))
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(timeoutSec))
                .retry(retry)
                .block();

        if (result == null || result.length == 0) {
            throw new RuntimeException("remove.bg API returned empty response");
        }

        Long resultFileId = fileService.saveBytes(
                result, 
                "image/png", 
                RelationType.AD, 
                originalFileId, 
                FileType.PREVIEW
        );
        
        log.info("[remove.bg] Success: originalFileId={} -> resultFileId={}", originalFileId, resultFileId);
        return resultFileId;
    }

    /**
     * 더미 배경 제거 (원본 파일 그대로 복사)
     */
    private Long removeBackgroundDummy(Long originalFileId, Map<String, Object> opts) throws Exception {
        log.info("[RemoveBgService] Using dummy mode for fileId: {}", originalFileId);
        
        byte[] original = fileService.loadAsBytes(originalFileId);
        log.info("[RemoveBgService] Loaded original file, size: {} bytes", original.length);
        
        Long resultFileId = fileService.saveBytes(
                original,
                "image/png",
                RelationType.AD,
                originalFileId,
                FileType.PREVIEW
        );
        
        log.info("[RemoveBgService] Dummy processing complete: originalFileId={} -> resultFileId={}", 
                originalFileId, resultFileId);
        return resultFileId;
    }

    private String write(Map<String, Object> m) {
        try { 
            return om.writeValueAsString(m); 
        } catch (Exception e) { 
            return "{}"; 
        }
    }

    public static record RemoveBgJobEvent(UUID jobId, Map<String, Object> params) {}
}
