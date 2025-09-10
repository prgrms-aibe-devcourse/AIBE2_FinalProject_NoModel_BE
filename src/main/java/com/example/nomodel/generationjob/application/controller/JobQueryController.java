package com.example.nomodel.generationjob.application.controller;

import com.example.nomodel.generationjob.application.service.GenerationJobService;
import com.example.nomodel.generationjob.application.dto.JobViewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/generate/jobs")
public class JobQueryController {

    private final GenerationJobService jobs;
    private final com.example.nomodel.file.domain.repository.FileJpaRepository fileRepo;

    @GetMapping("/{jobId}")
    public ResponseEntity<?> view(@PathVariable UUID jobId) {
        var job = jobs.view(jobId);

        String resultUrl = (job.getResultFileId() == null) ? null
                : fileRepo.findById(job.getResultFileId()).map(f -> f.getFileUrl()).orElse(null);

        String inputUrl = (job.getInputFileId() == null) ? null
                : fileRepo.findById(job.getInputFileId()).map(f -> f.getFileUrl()).orElse(null);

        return ResponseEntity.ok(JobViewResponse.from(job, resultUrl, inputUrl));
    }
}
