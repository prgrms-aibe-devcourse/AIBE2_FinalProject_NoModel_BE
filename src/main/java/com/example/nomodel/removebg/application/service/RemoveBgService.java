package com.example.nomodel.removebg.application.service;

import com.example.nomodel.generationjob.application.service.GenerationJobService;
import com.example.nomodel.removebg.application.service.provider.BackgroundRemovalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveBgService {

    private final GenerationJobService jobs;
    private final BackgroundRemovalService provider;
    private final ObjectMapper om;

    @Transactional
    public UUID enqueue(Long ownerId, Long fileId) {
        Map<String, Object> params = Map.of("retry", 2); // 재시도 기본값
        String paramsJson = write(params);

        var job = jobs.enqueueRemoveBg(ownerId, fileId, paramsJson);

        // 비동기 실행 (GenerationJobService 에서 @Async 로 실행)
        jobs.runRemoveBg(job.getId(),
                (inputFileId, opts) -> {
                    try {
                        return provider.removeBackground(inputFileId, opts);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                params
        );

        return job.getId();
    }

    private String write(Map<String, Object> m) {
        try { return om.writeValueAsString(m); } catch (Exception e) { return "{}"; }
    }
}