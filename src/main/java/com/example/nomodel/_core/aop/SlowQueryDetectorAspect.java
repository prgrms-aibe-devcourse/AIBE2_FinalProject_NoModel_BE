package com.example.nomodel._core.aop;

import com.example.nomodel._core.logging.StructuredLogger;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Repository 레이어 Slow Query 감지 AOP
 * 느린 쿼리 자동 감지 및 보고
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SlowQueryDetectorAspect {

    private final MeterRegistry meterRegistry;
    private final StructuredLogger structuredLogger;
    
    // 느린 쿼리 임계값 (밀리초) - application.yml에서 설정 가능
    @Value("${monitoring.slow-query.threshold-ms:1000}")
    private long slowQueryThresholdMs;
    
    @Value("${monitoring.slow-query.warning-threshold-ms:500}")
    private long warningThresholdMs;
    
    // 최근 느린 쿼리 기록 (최대 100개 보관)
    private final ConcurrentLinkedQueue<SlowQueryRecord> slowQueryHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * Repository 인터페이스의 모든 메소드 대상
     */
    @Pointcut("@within(org.springframework.stereotype.Repository) || " +
              "@within(org.springframework.data.jpa.repository.JpaRepository)")
    public void repositoryMethods() {}
    
    /**
     * JPA Repository 패키지 내의 모든 메소드
     */
    @Pointcut("execution(* com.example.nomodel..repository..*.*(..))")
    public void repositoryPackage() {}
    
    /**
     * 최종 Pointcut: Repository 메소드
     */
    @Pointcut("repositoryMethods() || repositoryPackage()")
    public void queryMethods() {}
    
    /**
     * Query 실행 시간 측정 및 Slow Query 감지
     */
    @Around("queryMethods()")
    public Object detectSlowQueries(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String queryIdentifier = className + "." + methodName;
        
        // 파라미터 정보 수집
        Map<String, Object> parameters = extractParameters(joinPoint);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 쿼리 실행
            Object result = joinPoint.proceed();
            
            // 실행 시간 계산
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Prometheus 메트릭 기록 (메트릭만)
            recordQueryMetrics(queryIdentifier, executionTime, true);
            
            // ELK Stack 구조화 로깅
            Map<String, Object> queryDetails = createQueryDetails(queryIdentifier, parameters, executionTime, true);
            structuredLogger.logQueryAnalysis(
                log, queryIdentifier, determineQueryType(queryIdentifier), executionTime, queryDetails
            );
            
            // Slow Query 체크 (알림용)
            checkSlowQuery(queryIdentifier, executionTime, parameters);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Prometheus 메트릭 기록 (메트릭만)
            recordQueryMetrics(queryIdentifier, executionTime, false);
            
            // ELK Stack 구조화 에러 로깅
            Map<String, Object> queryDetails = createQueryDetails(queryIdentifier, parameters, executionTime, false);
            queryDetails.put("errorMessage", e.getMessage());
            queryDetails.put("errorType", e.getClass().getSimpleName());
            
            structuredLogger.logQueryAnalysis(
                log, queryIdentifier, determineQueryType(queryIdentifier), executionTime, queryDetails
            );
            
            throw e;
        }
    }
    
    /**
     * 쿼리 메트릭 기록 (Prometheus용)
     */
    private void recordQueryMetrics(String queryIdentifier, long executionTime, boolean success) {
        meterRegistry.timer("repository.query.execution",
                "query", queryIdentifier,
                "status", success ? "success" : "failure")
                .record(executionTime, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 쿼리 세부 정보 생성 (ELK Stack용)
     */
    private Map<String, Object> createQueryDetails(String queryIdentifier, Map<String, Object> parameters, 
                                                  long executionTime, boolean success) {
        Map<String, Object> details = new HashMap<>();
        
        details.put("success", success);
        details.put("parameterCount", parameters.size());
        details.put("hasParameters", !parameters.isEmpty());
        
        // 성능 카테고리 분류
        if (executionTime > slowQueryThresholdMs) {
            details.put("performanceCategory", "CRITICAL");
        } else if (executionTime > warningThresholdMs) {
            details.put("performanceCategory", "WARNING");
        } else {
            details.put("performanceCategory", "NORMAL");
        }
        
        // 쿼리 패턴 분석
        String methodName = queryIdentifier.toLowerCase();
        if (methodName.contains("findall") || methodName.contains("getall")) {
            details.put("queryPattern", "BULK_SELECT");
        } else if (methodName.contains("findby")) {
            details.put("queryPattern", "CONDITIONAL_SELECT");
        } else if (methodName.contains("save") || methodName.contains("insert")) {
            details.put("queryPattern", "INSERT_UPDATE");
        } else if (methodName.contains("delete")) {
            details.put("queryPattern", "DELETE");
        } else {
            details.put("queryPattern", "UNKNOWN");
        }
        
        // 최적화 제안 (ELK에서 분석용)
        if (executionTime > slowQueryThresholdMs) {
            details.put("optimizationSuggestions", generateOptimizationSuggestions(queryIdentifier, executionTime));
        }
        
        return details;
    }
    
    /**
     * 쿼리 유형 결정
     */
    private String determineQueryType(String queryIdentifier) {
        String methodName = queryIdentifier.toLowerCase();
        if (methodName.contains("find") || methodName.contains("get") || methodName.contains("select")) {
            return "SELECT";
        } else if (methodName.contains("save") || methodName.contains("insert") || methodName.contains("update")) {
            return "INSERT_UPDATE";
        } else if (methodName.contains("delete")) {
            return "DELETE";
        } else {
            return "UNKNOWN";
        }
    }
    
    /**
     * 최적화 제안 생성 (ELK Stack용)
     */
    private List<String> generateOptimizationSuggestions(String queryIdentifier, long executionTime) {
        List<String> suggestions = new ArrayList<>();
        
        // 메소드명 기반 제안
        String methodName = queryIdentifier.toLowerCase();
        
        if (methodName.contains("findall") || methodName.contains("getall")) {
            suggestions.add("Consider using pagination for large result sets");
        }
        
        if (methodName.contains("findby") && !methodName.contains("id")) {
            suggestions.add("Check if an index exists on the search column");
        }
        
        if (executionTime > 5000) {
            suggestions.add("Query exceeds 5 seconds - consider query optimization or caching");
        }
        
        if (methodName.contains("join") || methodName.contains("fetch")) {
            suggestions.add("Review JOIN strategy - consider lazy loading or batch fetching");
        }
        
        return suggestions;
    }
    
    /**
     * Slow Query 체크 및 처리
     */
    private void checkSlowQuery(String queryIdentifier, long executionTime, Map<String, Object> parameters) {
        if (executionTime > slowQueryThresholdMs) {
            // 심각한 Slow Query
            handleSlowQuery(queryIdentifier, executionTime, parameters, "CRITICAL");
            
            // 알림 메트릭 증가
            meterRegistry.counter("repository.slow.query.critical",
                    "query", queryIdentifier).increment();
            
        } else if (executionTime > warningThresholdMs) {
            // 경고 수준 Slow Query
            log.warn("[SLOW QUERY WARNING] {} took {}ms (warning threshold: {}ms)",
                    queryIdentifier, executionTime, warningThresholdMs);
            
            // 경고 메트릭 증가
            meterRegistry.counter("repository.slow.query.warning",
                    "query", queryIdentifier).increment();
        }
    }
    
    /**
     * Slow Query 처리
     */
    private void handleSlowQuery(String queryIdentifier, long executionTime, 
                                 Map<String, Object> parameters, String severity) {
        // 로그 기록
        log.error("[SLOW QUERY {}] {} took {}ms (threshold: {}ms)",
                severity, queryIdentifier, executionTime, slowQueryThresholdMs);
        
        // 파라미터 로깅
        if (!parameters.isEmpty()) {
            log.error("[SLOW QUERY PARAMS] {}", formatParameters(parameters));
        }
        
        // 분석 제안 로깅
        logOptimizationSuggestions(queryIdentifier, executionTime);
        
        // 히스토리에 추가
        addToHistory(new SlowQueryRecord(
                queryIdentifier,
                executionTime,
                parameters,
                severity,
                LocalDateTime.now()
        ));
        
        // 임계값을 크게 초과한 경우 스택 트레이스 기록
        if (executionTime > slowQueryThresholdMs * 3) {
            logStackTrace();
        }
    }
    
    /**
     * 최적화 제안 로깅
     */
    private void logOptimizationSuggestions(String queryIdentifier, long executionTime) {
        List<String> suggestions = new ArrayList<>();
        
        // 메소드명 기반 제안
        String methodName = queryIdentifier.toLowerCase();
        
        if (methodName.contains("findall") || methodName.contains("getall")) {
            suggestions.add("Consider using pagination for large result sets");
        }
        
        if (methodName.contains("findby") && !methodName.contains("id")) {
            suggestions.add("Check if an index exists on the search column");
        }
        
        if (executionTime > 5000) {
            suggestions.add("Query exceeds 5 seconds - consider query optimization or caching");
        }
        
        if (methodName.contains("join") || methodName.contains("fetch")) {
            suggestions.add("Review JOIN strategy - consider lazy loading or batch fetching");
        }
        
        if (!suggestions.isEmpty()) {
            log.error("[OPTIMIZATION SUGGESTIONS] for {}:", queryIdentifier);
            suggestions.forEach(s -> log.error("  - {}", s));
        }
    }
    
    /**
     * 히스토리에 추가
     */
    private void addToHistory(SlowQueryRecord record) {
        slowQueryHistory.offer(record);
        
        // 크기 제한
        while (slowQueryHistory.size() > MAX_HISTORY_SIZE) {
            slowQueryHistory.poll();
        }
        
        // 주기적으로 요약 보고 (10개마다)
        if (slowQueryHistory.size() % 10 == 0) {
            generateSlowQueryReport();
        }
    }
    
    /**
     * Slow Query 리포트 생성
     */
    private void generateSlowQueryReport() {
        if (slowQueryHistory.isEmpty()) {
            return;
        }
        
        Map<String, List<SlowQueryRecord>> groupedByQuery = slowQueryHistory.stream()
                .collect(Collectors.groupingBy(SlowQueryRecord::getQueryIdentifier));
        
        log.warn("=== SLOW QUERY REPORT ===");
        log.warn("Total slow queries in history: {}", slowQueryHistory.size());
        
        groupedByQuery.forEach((query, records) -> {
            long avgTime = (long) records.stream()
                    .mapToLong(SlowQueryRecord::getExecutionTime)
                    .average()
                    .orElse(0);
            
            long maxTime = records.stream()
                    .mapToLong(SlowQueryRecord::getExecutionTime)
                    .max()
                    .orElse(0);
            
            log.warn("Query: {} - Count: {}, Avg: {}ms, Max: {}ms",
                    query, records.size(), avgTime, maxTime);
        });
        log.warn("========================");
    }
    
    /**
     * 파라미터 추출
     */
    private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] params = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < params.length && i < args.length; i++) {
            String paramName = params[i].getName();
            Object paramValue = args[i];
            
            // 민감한 정보 마스킹
            if (paramName.toLowerCase().contains("password") || 
                paramName.toLowerCase().contains("secret")) {
                paramValue = "***MASKED***";
            }
            
            parameters.put(paramName, paramValue);
        }
        
        return parameters;
    }
    
    /**
     * 파라미터 포맷팅
     */
    private String formatParameters(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
    
    /**
     * 스택 트레이스 로깅
     */
    private void logStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.error("[SLOW QUERY STACK TRACE]");
        Arrays.stream(stackTrace)
                .limit(10)
                .filter(element -> element.getClassName().startsWith("com.example.nomodel"))
                .forEach(element -> log.error("  at {}", element));
    }
    
    /**
     * Method 정보 추출
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
    
    /**
     * Slow Query 기록 클래스
     */
    private static class SlowQueryRecord {
        private final String queryIdentifier;
        private final long executionTime;
        private final Map<String, Object> parameters;
        private final String severity;
        private final LocalDateTime timestamp;
        
        public SlowQueryRecord(String queryIdentifier, long executionTime,
                              Map<String, Object> parameters, String severity,
                              LocalDateTime timestamp) {
            this.queryIdentifier = queryIdentifier;
            this.executionTime = executionTime;
            this.parameters = new HashMap<>(parameters);
            this.severity = severity;
            this.timestamp = timestamp;
        }
        
        public String getQueryIdentifier() {
            return queryIdentifier;
        }
        
        public long getExecutionTime() {
            return executionTime;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}