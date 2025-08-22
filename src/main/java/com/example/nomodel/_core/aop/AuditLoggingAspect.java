package com.example.nomodel._core.aop;

import com.example.nomodel._core.aop.annotation.Auditable;
import com.example.nomodel._core.logging.StructuredLogger;
import com.example.nomodel._core.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 비즈니스 로직 감사(Audit) 로깅 AOP
 * @Auditable 어노테이션이 적용된 메소드의 실행을 추적하고 감사 로그를 생성
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final StructuredLogger structuredLogger;
    
    // 감사 통계 저장
    private final ConcurrentHashMap<String, AuditStatistics> auditStats = new ConcurrentHashMap<>();
    
    // 민감한 필드명 목록
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwd", "pwd", "secret", "token", "key", "credential",
            "ssn", "social", "card", "credit", "account", "bank", "pin"
    );
    
    /**
     * @Auditable 어노테이션이 적용된 메소드 대상
     */
    @Pointcut("@annotation(com.example.nomodel._core.aop.annotation.Auditable)")
    public void auditableMethod() {}
    
    /**
     * 클래스 레벨에서 @Auditable이 적용된 경우의 메소드들
     */
    @Pointcut("@within(com.example.nomodel._core.aop.annotation.Auditable)")
    public void auditableClass() {}
    
    /**
     * 최종 Pointcut
     */
    @Pointcut("auditableMethod() || auditableClass()")
    public void auditableTarget() {}
    
    /**
     * 메소드 실행 전후 감사 로깅
     */
    @Around("auditableTarget()")
    public Object auditExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Auditable auditable = getAuditableAnnotation(joinPoint);
        if (auditable == null) {
            return joinPoint.proceed();
        }
        
        // 감사 정보 수집
        AuditContext context = createAuditContext(joinPoint, auditable);
        String auditId = generateAuditId();
        
        // ELK Stack 최적화 감사 로깅 (시작)
        logAuditStart(auditId, context, joinPoint, auditable);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 메소드 실행
            Object result = joinPoint.proceed();
            
            // ELK Stack 최적화 감사 로깅 (성공)
            long executionTime = System.currentTimeMillis() - startTime;
            logAuditSuccess(auditId, context, result, executionTime, auditable);
            
            // 메트릭 기록
            recordAuditMetrics(context, "SUCCESS", executionTime);
            
            // 통계 업데이트
            updateAuditStatistics(context, true, executionTime);
            
            return result;
            
        } catch (Exception e) {
            // ELK Stack 최적화 감사 로깅 (실패)
            long executionTime = System.currentTimeMillis() - startTime;
            logAuditFailure(auditId, context, e, executionTime);
            
            // 메트릭 기록
            recordAuditMetrics(context, "FAILURE", executionTime);
            
            // 통계 업데이트
            updateAuditStatistics(context, false, executionTime);
            
            throw e;
        }
    }
    
    /**
     * ELK Stack 최적화 감사 로깅 (시작)
     */
    private void logAuditStart(String auditId, AuditContext context, ProceedingJoinPoint joinPoint, Auditable auditable) {
        // ELK Stack 구조화 로깅 사용
        Map<String, Object> details = new HashMap<>();
        details.put("auditId", auditId);
        details.put("event", "AUDIT_START");
        details.put("method", context.getMethodName());
        details.put("className", context.getClassName());
        details.put("clientIp", context.getClientIp());
        details.put("userAgent", context.getUserAgent());
        details.put("sessionId", context.getSessionId());
        details.put("userRoles", context.getUserRoles());
        
        // 파라미터 로깅
        if (context.isLogParameters()) {
            details.put("parameters", maskSensitiveData(extractParameters(joinPoint)));
        }
        
        structuredLogger.logAudit(
            log, "AUDIT_START", context.getAction(), context.getCategory(),
            context.getLevel().name(), context.getUserId(), context.getUserName(), details
        );
    }
    
    /**
     * ELK Stack 최적화 감사 로깅 (성공)
     */
    private void logAuditSuccess(String auditId, AuditContext context, Object result, 
                                long executionTime, Auditable auditable) {
        Map<String, Object> details = new HashMap<>();
        details.put("auditId", auditId);
        details.put("event", "AUDIT_SUCCESS");
        details.put("executionTimeMs", executionTime);
        details.put("status", "SUCCESS");
        
        // 결과 로깅
        if (auditable.logResult() && result != null) {
            details.put("result", maskSensitiveData(result));
        }
        
        // 성능 카테고리 분류
        if (executionTime > 2000) {
            details.put("performanceCategory", "SLOW");
        } else if (executionTime > 1000) {
            details.put("performanceCategory", "WARNING");
        } else {
            details.put("performanceCategory", "NORMAL");
        }
        
        structuredLogger.logAudit(
            log, "AUDIT_SUCCESS", context.getAction(), context.getCategory(),
            context.getLevel().name(), context.getUserId(), context.getUserName(), details
        );
    }
    
    /**
     * ELK Stack 최적화 감사 로깅 (실패)
     */
    private void logAuditFailure(String auditId, AuditContext context, Exception e, long executionTime) {
        Map<String, Object> details = new HashMap<>();
        details.put("auditId", auditId);
        details.put("event", "AUDIT_FAILURE");
        details.put("executionTimeMs", executionTime);
        details.put("status", "FAILURE");
        details.put("errorType", e.getClass().getSimpleName());
        details.put("errorMessage", e.getMessage());
        
        // 오류 심각도 분류
        if (e instanceof SecurityException) {
            details.put("errorSeverity", "CRITICAL");
        } else if (e instanceof IllegalArgumentException) {
            details.put("errorSeverity", "MEDIUM");
        } else {
            details.put("errorSeverity", "HIGH");
        }
        
        structuredLogger.logAudit(
            log, "AUDIT_FAILURE", context.getAction(), context.getCategory(),
            context.getLevel().name(), context.getUserId(), context.getUserName(), details
        );
    }
    
    /**
     * 감사 컨텍스트 생성
     */
    private AuditContext createAuditContext(ProceedingJoinPoint joinPoint, Auditable auditable) {
        Method method = getMethod(joinPoint);
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        
        // 사용자 정보 추출
        String userId = "ANONYMOUS";
        String userName = "ANONYMOUS";
        Set<String> userRoles = new HashSet<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                userId = String.valueOf(userDetails.getMemberId());
                userName = userDetails.getUsername();
                userRoles = userDetails.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toSet());
            } else {
                userName = authentication.getName();
            }
        }
        
        // HTTP 요청 정보 추출
        String clientIp = "UNKNOWN";
        String userAgent = "UNKNOWN";
        String sessionId = "UNKNOWN";
        
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            clientIp = getClientIp(request);
            userAgent = request.getHeader("User-Agent");
            sessionId = request.getSession(false) != null ? request.getSession().getId() : "NONE";
        }
        
        // 액션명 결정
        String action = auditable.action().isEmpty() ? 
                className + "." + methodName : auditable.action();
        
        return new AuditContext(
                action,
                auditable.category(),
                auditable.level(),
                userId,
                userName,
                userRoles,
                className + "." + methodName,
                className,
                clientIp,
                userAgent,
                sessionId,
                auditable.logParameters()
        );
    }
    
    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 민감한 데이터 마스킹
     */
    private Object maskSensitiveData(Object data) {
        if (data == null) {
            return null;
        }
        
        try {
            String json = objectMapper.writeValueAsString(data);
            // 간단한 민감 데이터 마스킹 (실제로는 더 정교한 로직 필요)
            for (String field : SENSITIVE_FIELDS) {
                json = json.replaceAll("\"" + field + "\"\\s*:\\s*\"[^\"]*\"", 
                                     "\"" + field + "\":\"***MASKED***\"");
            }
            return json;
        } catch (Exception e) {
            return data.toString();
        }
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
            parameters.put(params[i].getName(), args[i]);
        }
        
        return parameters;
    }
    
    /**
     * 메트릭 기록
     */
    private void recordAuditMetrics(AuditContext context, String status, long executionTime) {
        meterRegistry.timer("audit.execution",
                "action", context.getAction(),
                "category", context.getCategory(),
                "level", context.getLevel().name(),
                "status", status)
                .record(executionTime, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("audit.count",
                "action", context.getAction(),
                "category", context.getCategory(),
                "status", status)
                .increment();
    }
    
    /**
     * 통계 업데이트
     */
    private void updateAuditStatistics(AuditContext context, boolean success, long executionTime) {
        auditStats.compute(context.getAction(), (key, stats) -> {
            if (stats == null) {
                stats = new AuditStatistics(context.getAction());
            }
            stats.recordExecution(success, executionTime);
            return stats;
        });
    }
    
    /**
     * 감사 로그 포맷팅
     */
    private String formatAuditLog(Map<String, Object> auditLog) {
        try {
            return objectMapper.writeValueAsString(auditLog);
        } catch (Exception e) {
            return auditLog.toString();
        }
    }
    
    /**
     * 감사 ID 생성
     */
    private String generateAuditId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * @Auditable 어노테이션 추출
     */
    private Auditable getAuditableAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 메소드 레벨 어노테이션 우선
        Auditable methodAuditable = method.getAnnotation(Auditable.class);
        if (methodAuditable != null) {
            return methodAuditable;
        }
        
        // 클래스 레벨 어노테이션
        return method.getDeclaringClass().getAnnotation(Auditable.class);
    }
    
    /**
     * Method 정보 추출
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
    
    /**
     * 감사 컨텍스트 클래스
     */
    private static class AuditContext {
        private final String action;
        private final String category;
        private final Auditable.AuditLevel level;
        private final String userId;
        private final String userName;
        private final Set<String> userRoles;
        private final String methodName;
        private final String className;
        private final String clientIp;
        private final String userAgent;
        private final String sessionId;
        private final boolean logParameters;
        
        public AuditContext(String action, String category, Auditable.AuditLevel level,
                           String userId, String userName, Set<String> userRoles,
                           String methodName, String className, String clientIp,
                           String userAgent, String sessionId, boolean logParameters) {
            this.action = action;
            this.category = category;
            this.level = level;
            this.userId = userId;
            this.userName = userName;
            this.userRoles = userRoles;
            this.methodName = methodName;
            this.className = className;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.sessionId = sessionId;
            this.logParameters = logParameters;
        }
        
        // Getters
        public String getAction() { return action; }
        public String getCategory() { return category; }
        public Auditable.AuditLevel getLevel() { return level; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public Set<String> getUserRoles() { return userRoles; }
        public String getMethodName() { return methodName; }
        public String getClassName() { return className; }
        public String getClientIp() { return clientIp; }
        public String getUserAgent() { return userAgent; }
        public String getSessionId() { return sessionId; }
        public boolean isLogParameters() { return logParameters; }
    }
    
    /**
     * 감사 통계 클래스
     */
    private static class AuditStatistics {
        private final String action;
        private long totalCount = 0;
        private long successCount = 0;
        private long failureCount = 0;
        private long totalTime = 0;
        
        public AuditStatistics(String action) {
            this.action = action;
        }
        
        public synchronized void recordExecution(boolean success, long executionTime) {
            totalCount++;
            totalTime += executionTime;
            
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        public String getAction() { return action; }
        public long getTotalCount() { return totalCount; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public long getAverageTime() { return totalCount > 0 ? totalTime / totalCount : 0; }
    }
}