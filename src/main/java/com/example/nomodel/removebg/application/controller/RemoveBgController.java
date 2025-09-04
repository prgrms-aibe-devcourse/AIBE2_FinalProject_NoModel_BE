package com.example.nomodel.removebg.application.controller;

import com.example.nomodel.generationjob.application.service.GenerationJobService;
import com.example.nomodel.removebg.application.dto.RemoveBgRequest;
import com.example.nomodel.removebg.application.dto.RemoveBgResponse;
import com.example.nomodel.removebg.application.service.RemoveBgService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate")
@RequiredArgsConstructor
public class RemoveBgController {

    private final RemoveBgService app;

    @PostMapping("/remove-bg")
    public ResponseEntity<?> remove(@RequestBody RemoveBgRequest req,
                                    @RequestHeader(name="X-User-Id", required=false) Long userId) {
        // 임시: 인증 붙기 전까지 헤더로 넘기거나 0L로 대체
        var jobId = app.enqueue(userId == null ? 0L : userId, req.fileId());
        return ResponseEntity.accepted().body(new RemoveBgResponse(jobId));
    }
}
