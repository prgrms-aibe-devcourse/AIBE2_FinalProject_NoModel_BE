package com.example.nomodel.compose.application.controller;

import com.example.nomodel.compose.application.dto.ComposeRequest;
import com.example.nomodel.compose.application.dto.ComposeResponse;
import com.example.nomodel.generationjob.application.service.GenerationJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/generate")
public class ComposeController {

    private final GenerationJobService jobService;

    @PostMapping("/compose")
    public ResponseEntity<?> compose(@RequestBody ComposeRequest req,
                                     @RequestHeader(name="X-User-Id", required=false) Long userId) {
        String jobId = jobService.enqueueCompose(
                userId == null ? 0L : userId,
                req.fileId(),
                req.prompt(),
                req.mode()
        );
        return ResponseEntity.accepted().body(new ComposeResponse(jobId));
    }
}
