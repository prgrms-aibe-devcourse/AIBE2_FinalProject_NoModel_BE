package com.example.nomodel._core.aop;

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

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller 레이어 AOP
 * API 요청/응답 로깅 및 성능 모니터링
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerAspect {

    private final StructuredLogger structuredLogger;

    private static final int ERROR_STACK_TRACE_LIMIT = 3;
    private static final String LOG_FORMAT_REQUEST = "[API Request] {} {} - Method: {}";
    private static final String LOG_FORMAT_RESPONSE = "[API Response] {} {} - Status: {} - Time: {}ms";
    private static final String LOG_FORMAT_ERROR = "[API Error] {} {} - Exception: {} - Time: {}ms";

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
     * Controller 메소드 실행 전후 로깅 및 성능 측정
     */
    @Around("pointcut()")
    public Object aroundLog(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // HTTP 요청 정보 추출
        HttpServletRequest request = getHttpServletRequest();
        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";

        // 메소드 정보 추출
        Method method = getMethod(proceedingJoinPoint);
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // StopWatch를 사용한 정확한 시간 측정
        StopWatch stopWatch = new StopWatch(methodName);
        stopWatch.start();

        // ELK Stack 최적화된 구조화 로깅
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("parameters", extractParameterInfo(proceedingJoinPoint.getArgs()));
        requestInfo.put("method", methodName);
        
        structuredLogger.logApiRequest(
            log, httpMethod, requestUri, methodName,
            0, 0, requestInfo
        );

        try {
            // 실제 메소드 실행
            Object response = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

            // 시간 측정 종료
            stopWatch.stop();

            // ELK Stack 최적화된 구조화 로깅 (응답)
            int statusCode = getStatusCode(response);
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("responseType", response.getClass().getSimpleName());
            if (log.isDebugEnabled()) {
                responseInfo.put("responseData", getResponseSummary(response));
            }
            
            structuredLogger.logApiRequest(
                log, httpMethod, requestUri, methodName,
                stopWatch.getTotalTimeMillis(), statusCode, responseInfo
            );

            return response;

        } catch (Exception e) {
            // 시간 측정 종료
            stopWatch.stop();

            // ELK Stack 최적화된 에러 로깅
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("exceptionType", e.getClass().getSimpleName());
            errorInfo.put("exceptionMessage", e.getMessage());
            if (log.isDebugEnabled()) {
                errorInfo.put("stackTrace", getShortStackTrace(e));
            }
            
            structuredLogger.logApiRequest(
                log, httpMethod, requestUri, methodName,
                stopWatch.getTotalTimeMillis(), 500, errorInfo
            );

            throw e;
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