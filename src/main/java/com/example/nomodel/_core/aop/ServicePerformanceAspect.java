package com.example.nomodel._core.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service 레이어 성능 모니터링 AOP
 * 메소드 실행 시간 측정 및 메트릭 수집
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ServicePerformanceAspect {

    private final MeterRegistry meterRegistry;
    
    // 메소드별 실행 통계 저장
    private final ConcurrentHashMap<String, MethodStatistics> methodStats = new ConcurrentHashMap<>();
    
    // 성능 임계값 (밀리초)
    private static final long SLOW_METHOD_THRESHOLD_MS = 1000;
    private static final long WARNING_METHOD_THRESHOLD_MS = 500;
    
    /**
     * Service 및 Component 어노테이션이 있는 클래스의 public 메소드 대상
     */
    @Pointcut("@within(org.springframework.stereotype.Service) || " +
              "@within(org.springframework.stereotype.Component)")
    public void serviceComponents() {}
    
    /**
     * Repository는 제외 (별도 AOP에서 처리)
     */
    @Pointcut("!@within(org.springframework.stereotype.Repository)")
    public void excludeRepository() {}
    
    /**
     * 서비스 패키지 내의 모든 메소드
     */
    @Pointcut("execution(* com.example.nomodel..service..*.*(..))")
    public void servicePackage() {}
    
    /**
     * 최종 Pointcut: Service 컴포넌트이면서 Repository가 아닌 메소드
     */
    @Pointcut("serviceComponents() && excludeRepository() && servicePackage()")
    public void serviceMethods() {}
    
    /**
     * Service 메소드 성능 측정
     */
    @Around("serviceMethods()")
    public Object measurePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String metricName = className + "." + methodName;
        
        // Micrometer Timer 시작
        Timer.Sample sample = Timer.start(meterRegistry);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 메소드 실행
            Object result = joinPoint.proceed();
            
            // 실행 시간 계산
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 메트릭 기록
            sample.stop(Timer.builder("service.method.execution")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "success")
                    .description("Service method execution time")
                    .register(meterRegistry));
            
            // 통계 업데이트
            updateStatistics(metricName, executionTime, true);
            
            // 성능 임계값 체크
            checkPerformanceThreshold(metricName, executionTime);
            
            // 디버그 로깅
            if (log.isDebugEnabled()) {
                log.debug("[Service Performance] {} executed in {}ms", metricName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 실패 메트릭 기록
            sample.stop(Timer.builder("service.method.execution")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "failure")
                    .tag("exception", e.getClass().getSimpleName())
                    .description("Service method execution time")
                    .register(meterRegistry));
            
            // 통계 업데이트
            updateStatistics(metricName, executionTime, false);
            
            // 에러 로깅
            log.error("[Service Performance] {} failed after {}ms: {}", 
                     metricName, executionTime, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * 메소드 통계 업데이트
     */
    private void updateStatistics(String metricName, long executionTime, boolean success) {
        methodStats.compute(metricName, (key, stats) -> {
            if (stats == null) {
                stats = new MethodStatistics(metricName);
            }
            stats.recordExecution(executionTime, success);
            return stats;
        });
        
        // 주기적으로 통계 로깅 (100번 호출마다)
        MethodStatistics stats = methodStats.get(metricName);
        if (stats.getTotalCount() % 100 == 0) {
            logStatistics(stats);
        }
    }
    
    /**
     * 성능 임계값 체크 및 경고
     */
    private void checkPerformanceThreshold(String metricName, long executionTime) {
        if (executionTime > SLOW_METHOD_THRESHOLD_MS) {
            log.warn("[SLOW METHOD] {} took {}ms (threshold: {}ms)", 
                    metricName, executionTime, SLOW_METHOD_THRESHOLD_MS);
            
            // 메트릭 카운터 증가
            meterRegistry.counter("service.method.slow", 
                    "method", metricName).increment();
                    
        } else if (executionTime > WARNING_METHOD_THRESHOLD_MS) {
            log.info("[PERFORMANCE WARNING] {} took {}ms (warning at: {}ms)", 
                    metricName, executionTime, WARNING_METHOD_THRESHOLD_MS);
        }
    }
    
    /**
     * 통계 정보 로깅
     */
    private void logStatistics(MethodStatistics stats) {
        log.info("[Service Statistics] {} - Total: {}, Success: {}, Failure: {}, " +
                "Avg Time: {}ms, Max Time: {}ms, Min Time: {}ms",
                stats.getMethodName(),
                stats.getTotalCount(),
                stats.getSuccessCount(),
                stats.getFailureCount(),
                stats.getAverageTime(),
                stats.getMaxTime(),
                stats.getMinTime());
    }
    
    /**
     * Method 정보 추출
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
    
    /**
     * 메소드 실행 통계 클래스
     */
    private static class MethodStatistics {
        private final String methodName;
        private long totalCount = 0;
        private long successCount = 0;
        private long failureCount = 0;
        private long totalTime = 0;
        private long maxTime = 0;
        private long minTime = Long.MAX_VALUE;
        
        public MethodStatistics(String methodName) {
            this.methodName = methodName;
        }
        
        public synchronized void recordExecution(long executionTime, boolean success) {
            totalCount++;
            totalTime += executionTime;
            
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
            
            if (executionTime > maxTime) {
                maxTime = executionTime;
            }
            if (executionTime < minTime) {
                minTime = executionTime;
            }
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public long getTotalCount() {
            return totalCount;
        }
        
        public long getSuccessCount() {
            return successCount;
        }
        
        public long getFailureCount() {
            return failureCount;
        }
        
        public long getAverageTime() {
            return totalCount > 0 ? totalTime / totalCount : 0;
        }
        
        public long getMaxTime() {
            return maxTime;
        }
        
        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }
    }
}