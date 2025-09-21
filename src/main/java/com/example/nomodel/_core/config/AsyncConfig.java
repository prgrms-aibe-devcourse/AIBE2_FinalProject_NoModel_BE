package com.example.nomodel._core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * 조회수 증가 등 비동기 작업을 위한 스레드 풀 구성
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-Job-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.error("Async task rejected: {}", r.toString());
        });
        executor.initialize();
        return executor;
    }

    /**
     * 조회수 증가 전용 스레드 풀
     * 별도의 스레드 풀로 격리하여 다른 비동기 작업과 분리
     */
    @Bean(name = "viewCountExecutor")
    public Executor viewCountExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 조회수 작업은 빈번하므로 스레드 풀을 작게 유지
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);  // 큐는 크게 설정
        executor.setThreadNamePrefix("ViewCount-");
        executor.setKeepAliveSeconds(60);

        // 큐가 가득 찼을 때 호출자 스레드에서 실행 (차단 방지)
        executor.setRejectedExecutionHandler((r, executor1) -> {
            // 로그만 남기고 버림 (조회수는 정확하지 않아도 됨)
            System.err.println("View count task rejected: " + r.toString());
        });

        executor.initialize();
        return executor;
    }
}
