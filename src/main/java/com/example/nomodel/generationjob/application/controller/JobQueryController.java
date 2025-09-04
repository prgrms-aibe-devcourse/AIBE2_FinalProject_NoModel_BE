package com.example.nomodel.generationjob.application.controller;

import com.example.nomodel.generationjob.application.service.GenerationJobService;
import com.example.nomodel.generationjob.application.dto.JobViewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/generate/jobs") // 현재 서버 context-path가 /api 이므로 최종 /api/api/generate/jobs
@RequiredArgsConstructor
public class JobQueryController {

    private final GenerationJobService jobs;

    @GetMapping("/{jobId}")
    public ResponseEntity<?> view(@PathVariable UUID jobId) {
        log.info("GET job {}", jobId);
        var job = jobs.view(jobId);          // 404 처리됨
        return ResponseEntity.ok(JobViewResponse.from(job));
    }
}
