package com.example.nomodel.model.application.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

/**
 * AIModel 인덱싱 스케줄러 테스트
 */
class AIModelIndexSchedulerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job aiModelIndexJob;
    
    private AIModelIndexScheduler scheduler;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new AIModelIndexScheduler(jobLauncher, aiModelIndexJob);
    }

    @Test
    void 배치_스케줄러_실행_테스트() throws Exception {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        given(jobLauncher.run(eq(aiModelIndexJob), any(JobParameters.class)))
                .willReturn(mockJobExecution);

        // When
        scheduler.runAIModelIndexBatch();

        // Then
        verify(jobLauncher, times(1)).run(eq(aiModelIndexJob), any(JobParameters.class));
        // JobParameters에 timestamp가 포함되는지 검증
        verify(jobLauncher).run(eq(aiModelIndexJob), 
            argThat(params -> params.getLocalDateTime("timestamp") != null));
    }

    @Test
    void 증분_동기화_스케줄러_실행_테스트() throws Exception {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        given(jobLauncher.run(eq(aiModelIndexJob), any(JobParameters.class)))
                .willReturn(mockJobExecution);

        // When
        scheduler.runIncrementalSync();

        // Then
        verify(jobLauncher, times(1)).run(eq(aiModelIndexJob), any(JobParameters.class));
        // 증분 동기화 파라미터들이 올바르게 설정되는지 검증
        verify(jobLauncher).run(eq(aiModelIndexJob), 
            argThat(params -> 
                "incremental".equals(params.getString("syncType")) &&
                params.getLocalDateTime("fromDateTime") != null
            ));
    }

    @Test
    void 배치_실행_실패_처리_테스트() throws Exception {
        // Given
        given(jobLauncher.run(eq(aiModelIndexJob), any(JobParameters.class)))
                .willThrow(new RuntimeException("배치 실행 실패"));

        // When & Then (예외 발생해도 스케줄러는 멈추지 않음)
        scheduler.runAIModelIndexBatch();
        
        verify(jobLauncher, times(1)).run(eq(aiModelIndexJob), any(JobParameters.class));
    }
}