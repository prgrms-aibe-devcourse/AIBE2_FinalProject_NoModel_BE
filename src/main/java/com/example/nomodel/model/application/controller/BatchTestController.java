package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 배치 테스트용 컨트롤러 (테스트 환경에만 존재)
 * 수동으로 배치 작업을 실행하여 테스트
 */
@Slf4j
@RestController
@RequestMapping("/batch/test")
@RequiredArgsConstructor
@Tag(name = "Model Batch Test", description = "AI 모델 배치 테스트 API")
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "local")
public class BatchTestController {

    private final JobLauncher jobLauncher;
    
    @Qualifier("aiModelIndexJob")
    private final Job aiModelIndexJob;

    @Operation(summary = "AIModel 배치 작업 수동 실행", 
               description = "AIModel 인덱싱 배치를 수동으로 실행합니다 (테스트용)")
    @PostMapping("/run-aimodel-index")
    public ResponseEntity<?> runAIModelIndexBatch() {
        try {
            log.info("AIModel 인덱싱 배치 수동 실행 시작");
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .addString("trigger", "manual-test")
                    .toJobParameters();
            
            jobLauncher.run(aiModelIndexJob, jobParameters);
            
            return ResponseEntity.ok(ApiUtils.success("AIModel 인덱싱 배치 실행 완료"));
            
        } catch (Exception e) {
            log.error("AIModel 인덱싱 배치 수동 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiUtils.error("배치 실행 실패: " + e.getMessage()));
        }
    }
}