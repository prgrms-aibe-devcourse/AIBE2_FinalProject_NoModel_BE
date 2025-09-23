package com.example.nomodel._core.aop;

import com.example.nomodel._core.aop.annotation.BusinessCritical;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel._core.logging.StructuredLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller 레이어 AOP
 * API 비즈니스 로직 중심 로깅 (HTTP 메트릭은 Actuator가 처리)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerAspect {

    private final StructuredLogger structuredLogger;

    private static final int ERROR_STACK_TRACE_LIMIT = 3;

    /**
     * Controller 패키지 내 모든 public 메소드를 대상으로 하는 Pointcut
     * RestController 또는 Controller 어노테이션이 있는 클래스만 대상
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || " +
              "within(@org.springframework.stereotype.Controller *)")
    public void controllerMethods() {}

    /**
     * API 패키지 하위의 모든 Controller 메소드 대상
     */
    @Pointcut("execution(* com.example.nomodel..controller..*.*(..)) || " +
              "execution(* com.example.nomodel..api..*.*(..))")
    public void apiMethods() {}

    /**
     * 최종 Pointcut: Controller 어노테이션이 있으면서 API 패키지에 속한 메소드
     */
    @Pointcut("controllerMethods() && apiMethods()")
    public void pointcut() {}

    /**
     * Controller 메소드 비즈니스 로직 로깅 (상세 로깅만, 메트릭은 Actuator가 처리)
     */
    @Around("pointcut()")
    public Object logBusinessRequests(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // HTTP 요청 정보 추출
        HttpServletRequest request = getHttpServletRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";

        // 메소드 정보 추출
        Method method = getMethod(proceedingJoinPoint);
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String fullMethodName = className + "." + methodName;
        String executionId = UUID.randomUUID().toString().substring(0, 8);

        // MDC에 메서드별 상세 정보 설정
        MDC.put("class_name", className);
        MDC.put("method_name", methodName);
        MDC.put("full_method_name", fullMethodName);
        MDC.put("layer", "Controller");
        MDC.put("execution_id", executionId);
        MDC.put("http_method", httpMethod);
        MDC.put("request_uri", requestUri);

        // StopWatch를 사용한 정확한 시간 측정
        StopWatch stopWatch = new StopWatch(fullMethodName);
        stopWatch.start();

        // 비즈니스 API인 경우만 상세 로깅 (일반적인 HTTP 메트릭은 Actuator가 처리)
        boolean isBusinessApi = isBusinessCriticalApi(proceedingJoinPoint);
        
        if (isBusinessApi) {
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("parameters", extractParameterInfo(proceedingJoinPoint.getArgs()));
            requestInfo.put("method", methodName);
            requestInfo.put("businessType", getBusinessDomain(proceedingJoinPoint).name());
            requestInfo.put("businessDomain", getBusinessDomain(proceedingJoinPoint).getDescription());
            requestInfo.put("criticalLevel", getCriticalLevel(proceedingJoinPoint).name());
            
            structuredLogger.logApiRequest(
                log, httpMethod, requestUri, methodName,
                0, 0, requestInfo
            );
        }

        try {
            // 실제 메소드 실행
            Object response = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

            // 시간 측정 종료
            stopWatch.stop();

            // MDC에 실행 결과 정보 추가
            MDC.put("execution_time_ms", String.valueOf(stopWatch.getTotalTimeMillis()));
            MDC.put("status", "SUCCESS");
            MDC.put("execution_category", categorizeExecutionTime(stopWatch.getTotalTimeMillis()));

            // 비즈니스 API인 경우만 상세 응답 로깅
            if (isBusinessApi) {
                int statusCode = getStatusCode(response);
                Map<String, Object> responseInfo = new HashMap<>();
                responseInfo.put("responseType", response.getClass().getSimpleName());
                responseInfo.put("businessType", getBusinessDomain(proceedingJoinPoint).name());
                responseInfo.put("businessDomain", getBusinessDomain(proceedingJoinPoint).getDescription());
                responseInfo.put("criticalLevel", getCriticalLevel(proceedingJoinPoint).name());
                responseInfo.put("executionCategory", categorizeExecutionTime(stopWatch.getTotalTimeMillis()));

                if (log.isDebugEnabled()) {
                    responseInfo.put("responseData", getResponseSummary(response));
                }

                structuredLogger.logApiRequest(
                    log, httpMethod, requestUri, fullMethodName,
                    stopWatch.getTotalTimeMillis(), statusCode, responseInfo
                );
            }

            // 일반 메서드 실행 로그 (성능 임계값 체크)
            if (stopWatch.getTotalTimeMillis() > 1000) {
                log.warn("메서드 완료 (느림): {}#{} [Controller] ({}ms)",
                         className, methodName, stopWatch.getTotalTimeMillis());
            } else {
                log.info("메서드 완료: {}#{} [Controller] ({}ms)",
                         className, methodName, stopWatch.getTotalTimeMillis());
            }

            return response;

        } catch (Exception e) {
            // 시간 측정 종료
            stopWatch.stop();

            // MDC에 에러 정보 추가
            MDC.put("execution_time_ms", String.valueOf(stopWatch.getTotalTimeMillis()));
            MDC.put("status", "ERROR");
            MDC.put("error_message", e.getMessage());
            MDC.put("error_class", e.getClass().getSimpleName());

            // API 에러는 항상 로깅 (비즈니스 영향도 분석용)
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("exceptionType", e.getClass().getSimpleName());
            errorInfo.put("exceptionMessage", e.getMessage());
            errorInfo.put("businessType", getBusinessDomain(proceedingJoinPoint).name());
            errorInfo.put("businessDomain", getBusinessDomain(proceedingJoinPoint).getDescription());
            errorInfo.put("criticalLevel", getCriticalLevel(proceedingJoinPoint).name());
            errorInfo.put("isBusinessCritical", isBusinessApi);

            if (log.isDebugEnabled()) {
                errorInfo.put("stackTrace", getShortStackTrace(e));
            }

            structuredLogger.logApiRequest(
                log, httpMethod, requestUri, fullMethodName,
                stopWatch.getTotalTimeMillis(), 500, errorInfo
            );

            // 일반 메서드 에러 로그
            log.error("메서드 에러: {}#{} [Controller] ({}ms) - {}",
                     className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage(), e);

            throw e;
        } finally {
            // MDC 정리
            MDC.clear();
        }
    }

    /**
     * HTTP 요청 객체 추출
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Method 정보 추출
     */
    private Method getMethod(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        return methodSignature.getMethod();
    }

    /**
     * 파라미터 정보 추출 (ELK Stack 구조화)
     */
    private Map<String, Object> extractParameterInfo(Object[] args) {
        Map<String, Object> paramInfo = new LinkedHashMap<>();
        
        if (args.length == 0) {
            return paramInfo;
        }

        List<Map<String, Object>> parameters = Arrays.stream(args)
            .filter(Objects::nonNull)
            .filter(arg -> !(arg instanceof HttpServletRequest))
            .map(arg -> {
                Map<String, Object> param = new HashMap<>();
                param.put("type", arg.getClass().getSimpleName());
                // 민감한 정보 체크 후 값 설정
                param.put("value", isSensitiveData(arg) ? "***MASKED***" : String.valueOf(arg));
                return param;
            })
            .collect(Collectors.toList());
        
        paramInfo.put("count", parameters.size());
        paramInfo.put("details", parameters);
        return paramInfo;
    }

    /**
     * HTTP 상태 코드 추출
     */
    private int getStatusCode(Object response) {
        if (response instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return 200;
    }

    /**
     * 응답 요약 정보 추출 (ELK Stack 구조화)
     */
    private Object getResponseSummary(Object response) {
        if (response instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("hasBody", body != null);
            
            if (body instanceof ApiUtils.ApiResult<?> apiResult) {
                summary.put("success", apiResult.error() == null);
                summary.put("errorCode", apiResult.error() != null ? apiResult.error() : null);
            }
            
            return summary;
        }
        
        return Map.of("type", response.getClass().getSimpleName());
    }
    
    /**
     * 비즈니스 중요 API 판단 (어노테이션 기반)
     */
    private boolean isBusinessCriticalApi(ProceedingJoinPoint joinPoint) {
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
     * BusinessCritical 어노테이션에서 비즈니스 도메인 추출
     */
    private BusinessCritical.BusinessDomain getBusinessDomain(ProceedingJoinPoint joinPoint) {
        Method method = getMethod(joinPoint);
        Class<?> targetClass = method.getDeclaringClass();
        
        // 메소드 레벨이 우선
        BusinessCritical methodAnnotation = method.getAnnotation(BusinessCritical.class);
        if (methodAnnotation != null) {
            return methodAnnotation.domain();
        }
        
        // 클래스 레벨 확인
        BusinessCritical classAnnotation = targetClass.getAnnotation(BusinessCritical.class);
        if (classAnnotation != null) {
            return classAnnotation.domain();
        }
        
        return BusinessCritical.BusinessDomain.GENERAL;
    }
    
    /**
     * BusinessCritical 어노테이션에서 중요도 레벨 추출
     */
    private BusinessCritical.CriticalLevel getCriticalLevel(ProceedingJoinPoint joinPoint) {
        Method method = getMethod(joinPoint);
        Class<?> targetClass = method.getDeclaringClass();
        
        // 메소드 레벨이 우선
        BusinessCritical methodAnnotation = method.getAnnotation(BusinessCritical.class);
        if (methodAnnotation != null) {
            return methodAnnotation.level();
        }
        
        // 클래스 레벨 확인
        BusinessCritical classAnnotation = targetClass.getAnnotation(BusinessCritical.class);
        if (classAnnotation != null) {
            return classAnnotation.level();
        }
        
        return BusinessCritical.CriticalLevel.MEDIUM;
    }
    
    
    /**
     * 실행 시간 카테고리 분류
     */
    private String categorizeExecutionTime(long executionTimeMs) {
        if (executionTimeMs > 2000) return "SLOW";
        if (executionTimeMs > 1000) return "WARNING";
        return "NORMAL";
    }
    
    /**
     * 민감한 데이터 체크
     */
    private boolean isSensitiveData(Object data) {
        String dataString = data.toString().toLowerCase();
        return dataString.contains("password") || 
               dataString.contains("token") || 
               dataString.contains("secret") ||
               dataString.contains("key");
    }

    /**
     * 짧은 스택 트레이스 생성
     */
    private String getShortStackTrace(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        int limit = Math.min(stackTrace.length, ERROR_STACK_TRACE_LIMIT);
        
        return Arrays.stream(stackTrace)
            .limit(limit)
            .map(StackTraceElement::toString)
            .collect(Collectors.joining("\n"));
    }
}