package com.example.nomodel._core.aop;

import com.example.nomodel._core.aop.annotation.BusinessCritical;
import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
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
import org.slf4j.MDC;

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
        String executionId = java.util.UUID.randomUUID().toString().substring(0, 8);

        // MDC에 Service 메서드 정보 설정
        MDC.put("class_name", className);
        MDC.put("method_name", methodName);
        MDC.put("full_method_name", operation);
        MDC.put("layer", "Service");
        MDC.put("execution_id", executionId);

        long startTime = System.currentTimeMillis();

        try {
            // 메소드 실행
            Object result = joinPoint.proceed();

            // 실행 시간 계산
            long executionTime = System.currentTimeMillis() - startTime;

            // MDC에 실행 결과 추가
            MDC.put("execution_time_ms", String.valueOf(executionTime));
            MDC.put("status", "SUCCESS");

            // 비즈니스 로직 특화 로깅 (@BusinessCritical 어노테이션 기반)
            if (executionTime > slowMethodThresholdMs || isBusinessCriticalMethod(joinPoint)) {
                Map<String, Object> context = createBusinessContext(joinPoint, executionTime, true);
                structuredLogger.logBusinessEvent(
                    log, "SERVICE_EXECUTION",
                    "Business service method executed: " + operation,
                    determineSeverity(executionTime), context
                );
            }

            // 일반 Service 메서드 실행 로그
            if (executionTime > slowMethodThresholdMs) {
                log.warn("메서드 완료 (느림): {}#{} [Service] ({}ms)",
                         className, methodName, executionTime);
            } else {
                log.debug("메서드 완료: {}#{} [Service] ({}ms)",
                         className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // MDC에 에러 정보 추가
            MDC.put("execution_time_ms", String.valueOf(executionTime));
            MDC.put("status", "ERROR");
            MDC.put("error_message", e.getMessage());
            MDC.put("error_class", e.getClass().getSimpleName());

            // 예외 심각도 결정 (비즈니스 예외 vs 시스템 오류)
            String severity = determineExceptionSeverity(e);

            // 심각도가 낮은 비즈니스 예외는 DEBUG 레벨로 처리
            if ("LOW".equals(severity)) {
                log.debug("비즈니스 예외 발생: {} - {}", operation, e.getMessage());
            } else {
                // 중요한 오류만 로깅
                Map<String, Object> context = createBusinessContext(joinPoint, executionTime, false);
                context.put("errorMessage", e.getMessage() != null ? e.getMessage() : "No error message available");
                context.put("errorType", e.getClass().getSimpleName());

                structuredLogger.logBusinessEvent(
                    log, "SERVICE_FAILURE",
                    "Business service method failed: " + operation,
                    severity, context
                );

                // 일반 Service 메서드 에러 로그
                log.error("메서드 에러: {}#{} [Service] ({}ms) - {}",
                         className, methodName, executionTime, e.getMessage(), e);
            }

            throw e;
        } finally {
            // MDC 정리
            MDC.clear();
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
     * 예외 심각도 결정
     * 정상적인 비즈니스 예외와 실제 시스템 오류를 구분
     */
    private String determineExceptionSeverity(Exception e) {
        // ApplicationException 중에서도 정상적인 비즈니스 상황인지 판단
        if (e instanceof ApplicationException) {
            ApplicationException appException = (ApplicationException) e;
            ErrorCode errorCode = appException.getErrorCode();
            
            // 정상적인 비즈니스 상황 (데이터가 없는 경우)
            if (isNormalBusinessCase(errorCode)) {
                return "LOW"; // DEBUG 레벨로 처리
            }
            
            // 클라이언트 오류 (잘못된 요청)
            if (isClientError(errorCode)) {
                return "MEDIUM"; // WARN 레벨
            }
            
            // 시스템 오류
            return "CRITICAL";
        }
        
        // 기타 시스템 예외는 모두 CRITICAL
        return "CRITICAL";
    }
    
    /**
     * 정상적인 비즈니스 상황 판단 (데이터가 없는 경우)
     */
    private boolean isNormalBusinessCase(ErrorCode errorCode) {
        return errorCode == ErrorCode.REVIEW_NOT_FOUND ||
               errorCode == ErrorCode.MODEL_NOT_FOUND; // 조회에서만 정상 상황
    }
    
    /**
     * 클라이언트 오류 판단 (잘못된 요청)
     */
    private boolean isClientError(ErrorCode errorCode) {
        return errorCode == ErrorCode.INVALID_REQUEST ||
               errorCode == ErrorCode.INVALID_ENUM_VALUE ||
               errorCode == ErrorCode.INVALID_RATING_VALUE ||
               errorCode == ErrorCode.DUPLICATE_REVIEW ||
               errorCode == ErrorCode.REVIEW_NOT_ALLOWED ||
               errorCode == ErrorCode.MEMBER_ALREADY_EXISTS ||
               errorCode == ErrorCode.EMAIL_ALREADY_EXISTS;
    }
    
    /**
     * Method 정보 추출
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}