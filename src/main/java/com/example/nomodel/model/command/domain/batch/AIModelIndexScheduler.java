package com.example.nomodel.model.command.domain.batch;

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
     * 매 5분마다 AIModel 증분 인덱싱 배치 실행
     * 최근 5분 이내 수정된 모델 처리 (BaseTimeEntity 특성상 새 모델도 포함)
     * 실시간 검색을 위해 짧은 간격으로 동기화
     */
    @Scheduled(fixedRate = 120000) // 2분 = 5 * 60 * 1000ms
    public void runIncrementalSync() {
        try {
            LocalDateTime fromDateTime = LocalDateTime.now().minusMinutes(5);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .addLocalDateTime("fromDateTime", fromDateTime)
                    .addString("syncType", "incremental")
                    .toJobParameters();
            
            log.info("AIModel 5분 증분 인덱싱 배치 시작 - fromDateTime: {} (updatedAt 기준)", fromDateTime);
            jobLauncher.run(aiModelIndexJob, jobParameters);
            
        } catch (Exception e) {
            log.error("AIModel 증분 인덱싱 배치 실행 실패", e);
        }
    }
}