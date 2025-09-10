package com.example.nomodel.model.application.scheduler;

import com.example.nomodel.model.domain.batch.AIModelIndexScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIModelIndexScheduler 단위 테스트")
class AIModelIndexSchedulerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job aiModelIndexJob;
    
    @InjectMocks
    private AIModelIndexScheduler scheduler;

    @Test
    @DisplayName("증분 인덱싱 스케줄러 실행 성공")
    void runIncrementalSync_Success() throws Exception {
        // given
        JobExecution mockJobExecution = mock(JobExecution.class);
        given(jobLauncher.run(eq(aiModelIndexJob), any(JobParameters.class)))
                .willReturn(mockJobExecution);

        // when
        scheduler.runIncrementalSync();

        // then
        then(jobLauncher).should(times(1)).run(eq(aiModelIndexJob), any(JobParameters.class));
        // 증분 처리 파라미터들이 올바르게 설정되는지 검증
        then(jobLauncher).should().run(eq(aiModelIndexJob), 
            argThat(params -> 
                "incremental".equals(params.getString("syncType")) &&
                params.getLocalDateTime("fromDateTime") != null &&
                params.getLocalDateTime("timestamp") != null
            ));
    }

    @Test
    @DisplayName("배치 실행 실패 시 예외 처리")
    void runIncrementalSync_JobLauncherFailure() throws Exception {
        // given
        given(jobLauncher.run(eq(aiModelIndexJob), any(JobParameters.class)))
                .willThrow(new RuntimeException("배치 실행 실패"));

        // when
        scheduler.runIncrementalSync();
        
        // then - 예외 발생해도 스케줄러는 멈추지 않음
        then(jobLauncher).should(times(1)).run(eq(aiModelIndexJob), any(JobParameters.class));
    }
}