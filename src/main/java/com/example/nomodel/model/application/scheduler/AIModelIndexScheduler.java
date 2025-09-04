package com.example.nomodel.model.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AIModel Elasticsearch 인덱싱 스케줄러
 * 주기적으로 배치 작업을 실행하여 데이터 동기화
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.batch.aimodel-index.enabled", havingValue = "true", matchIfMissing = false)
public class AIModelIndexScheduler {

    private final JobLauncher jobLauncher;
    private final Job aiModelIndexJob;
    
    public AIModelIndexScheduler(JobLauncher jobLauncher, 
                                @Qualifier("aiModelIndexJob") Job aiModelIndexJob) {
        this.jobLauncher = jobLauncher;
        this.aiModelIndexJob = aiModelIndexJob;
    }

    /**
     * 매시간 정각에 AIModel 인덱싱 배치 실행
     * cron: 0 0 * * * * (초 분 시 일 월 요일)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runAIModelIndexBatch() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();
            
            jobLauncher.run(aiModelIndexJob, jobParameters);
            
        } catch (Exception e) {
            log.error("AIModel 인덱싱 배치 실행 실패", e);
        }
    }

    /**
     * 매 30분마다 증분 동기화 (가벼운 작업)
     * 최근 30분 이내에 수정된 모델만 처리
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 30 * 60 * 1000ms
    public void runIncrementalSync() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .addString("syncType", "incremental")
                    .addLocalDateTime("fromDateTime", LocalDateTime.now().minusMinutes(30))
                    .toJobParameters();
            
            jobLauncher.run(aiModelIndexJob, jobParameters);
            
        } catch (Exception e) {
            log.error("AIModel 증분 동기화 실행 실패", e);
        }
    }
}