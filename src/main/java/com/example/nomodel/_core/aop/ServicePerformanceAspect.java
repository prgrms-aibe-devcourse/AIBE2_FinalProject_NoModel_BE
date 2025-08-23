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
import java.util.HashMap;
import java.util.Map;

/**
 * Service 레이어 비즈니스 로직 모니터링 AOP
 * @BusinessCritical 어노테이션 기반의 선별적 모니터링
 * Actuator와 중복되는 일반적인 메트릭은 제거하고, 비즈니스 특화 모니터링에만 집중
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ServicePerformanceAspect {

    private final StructuredLogger structuredLogger;
    
    // 비즈니스 로직 임계값 (일반적인 성능 메트릭은 Actuator에 위임)
    @Value("${monitoring.aop.service.slow-method-threshold-ms:2000}")
    private long slowMethodThresholdMs;
    
    /**
     * Service 및 Component 어노테이션이 있는 클래스의 public 메소드 대상
     * Repository는 제외 (별도 AOP에서 처리)
     */
    @Pointcut("@within(org.springframework.stereotype.Service) || " +
              "@within(org.springframework.stereotype.Component)")
    public void serviceComponents() {}
    
    @Pointcut("!@within(org.springframework.stereotype.Repository)")
    public void excludeRepository() {}
    
    @Pointcut("execution(* com.example.nomodel..service..*.*(..))")
    public void servicePackage() {}
    
    /**
     * 최종 Pointcut: Service 컴포넌트이면서 Repository가 아닌 메소드
     */
    @Pointcut("serviceComponents() && excludeRepository() && servicePackage()")
    public void serviceMethods() {}
    
    /**
     * 비즈니스 로직 특화 모니터링 (일반 메트릭은 Actuator가 처리)
     */
    @Around("serviceMethods()")
    public Object monitorBusinessLogic(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String operation = className + "." + methodName;
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 메소드 실행
            Object result = joinPoint.proceed();
            
            // 실행 시간 계산
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 비즈니스 로직 특화 로깅 (@BusinessCritical 어노테이션 기반)
            if (executionTime > slowMethodThresholdMs || isBusinessCriticalMethod(joinPoint)) {
                Map<String, Object> context = createBusinessContext(joinPoint, executionTime, true);
                structuredLogger.logBusinessEvent(
                    log, "SERVICE_EXECUTION", 
                    "Business service method executed: " + operation,
                    determineSeverity(executionTime), context
                );
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 비즈니스 로직 실패는 항상 로깅 (중요한 비즈니스 컨텍스트)
            Map<String, Object> context = createBusinessContext(joinPoint, executionTime, false);
            context.put("errorMessage", e.getMessage());
            context.put("errorType", e.getClass().getSimpleName());
            
            structuredLogger.logBusinessEvent(
                log, "SERVICE_FAILURE", 
                "Business service method failed: " + operation,
                "CRITICAL", context
            );
            
            throw e;
        }
    }
    
    /**
     * 비즈니스 중요 메소드 판단 (@BusinessCritical 어노테이션 기반)
     */
    private boolean isBusinessCriticalMethod(ProceedingJoinPoint joinPoint) {
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
     * 비즈니스 컨텍스트 생성 (ELK Stack 분석용)
     */
    private Map<String, Object> createBusinessContext(ProceedingJoinPoint joinPoint, 
                                                     long executionTime, boolean success) {
        Map<String, Object> context = new HashMap<>();
        Method method = getMethod(joinPoint);
        
        context.put("serviceClass", method.getDeclaringClass().getSimpleName());
        context.put("methodName", method.getName());
        context.put("executionTimeMs", executionTime);
        context.put("success", success);
        context.put("parameterCount", joinPoint.getArgs().length);
        context.put("isCriticalBusiness", isBusinessCriticalMethod(joinPoint));
        
        return context;
    }
    
    /**
     * 심각도 결정 (비즈니스 로직 기준)
     */
    private String determineSeverity(long executionTime) {
        if (executionTime > slowMethodThresholdMs * 2) return "CRITICAL";
        if (executionTime > slowMethodThresholdMs) return "HIGH";
        return "MEDIUM";
    }
    
    /**
     * Method 정보 추출
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}