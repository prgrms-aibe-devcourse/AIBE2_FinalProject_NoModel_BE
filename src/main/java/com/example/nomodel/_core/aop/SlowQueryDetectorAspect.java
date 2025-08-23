package com.example.nomodel._core.aop;

import com.example.nomodel._core.aop.annotation.BusinessCritical;
import com.example.nomodel._core.logging.StructuredLogger;
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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Repository 레이어 쿼리 분석 AOP
 * @BusinessCritical 어노테이션 기반의 느린 쿼리 감지 및 비즈니스 영향도 분석
 * (메트릭은 Actuator 연계)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SlowQueryDetectorAspect {

    private final StructuredLogger structuredLogger;
    // MeterRegistry 제거: JDBC 메트릭은 Actuator가 자동 수집
    
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
            
            // 비즈니스 중요 쿼리 또는 느린 쿼리만 로깅 (@BusinessCritical 어노테이션 기반)
            if (executionTime > warningThresholdMs || isBusinessCriticalQuery(joinPoint)) {
                Map<String, Object> queryDetails = createQueryDetails(queryIdentifier, parameters, executionTime, true);
                structuredLogger.logQueryAnalysis(
                    log, queryIdentifier, determineQueryType(queryIdentifier), executionTime, queryDetails
                );
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 쿼리 실패는 항상 로깅 (비즈니스 영향도 분석)
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
     * 비즈니스 중요 쿼리 판단 (@BusinessCritical 어노테이션 기반)
     */
    private boolean isBusinessCriticalQuery(ProceedingJoinPoint joinPoint) {
        Method method = getMethod(joinPoint);
        Class<?> targetClass = method.getDeclaringClass();
        
        // 1. 메소드 레벨 @BusinessCritical 어노테이션 확인
        BusinessCritical methodAnnotation = method.getAnnotation(BusinessCritical.class);
        if (methodAnnotation != null) {
            return true;
        }
        
        // 2. 클래스 레벨 @BusinessCritical 어노테이션 확인
        BusinessCritical classAnnotation = targetClass.getAnnotation(BusinessCritical.class);
        return classAnnotation != null;
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