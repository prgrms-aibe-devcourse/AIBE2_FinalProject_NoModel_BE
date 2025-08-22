package com.example.nomodel._core.aop;

import com.example.nomodel._core.utils.ApiUtils;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller 레이어 AOP
 * API 요청/응답 로깅 및 성능 모니터링
 */
@Slf4j
@Aspect
@Component
public class ControllerAspect {

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

        // 요청 로깅
        log.info(LOG_FORMAT_REQUEST, httpMethod, requestUri, methodName);
        logParameters(proceedingJoinPoint.getArgs());

        try {
            // 실제 메소드 실행
            Object response = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

            // 시간 측정 종료
            stopWatch.stop();

            // 응답 로깅
            String status = getResponseStatus(response);
            log.info(LOG_FORMAT_RESPONSE, httpMethod, requestUri, status, stopWatch.getTotalTimeMillis());
            
            if (log.isDebugEnabled()) {
                logResponse(response);
            }

            return response;

        } catch (Exception e) {
            // 시간 측정 종료
            stopWatch.stop();

            // 에러 로깅
            log.error(LOG_FORMAT_ERROR, httpMethod, requestUri, 
                     e.getClass().getSimpleName(), stopWatch.getTotalTimeMillis());
            
            if (log.isDebugEnabled()) {
                log.debug("Exception details: {}", e.getMessage());
                log.debug("Stack trace: {}", getShortStackTrace(e));
            }

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
     * 파라미터 로깅
     */
    private void logParameters(Object[] args) {
        if (!log.isDebugEnabled() || args.length == 0) {
            return;
        }

        String params = Arrays.stream(args)
            .filter(Objects::nonNull)
            .filter(arg -> !(arg instanceof HttpServletRequest)) // HTTP 요청 객체 제외
            .map(arg -> arg.getClass().getSimpleName() + ": " + arg.toString())
            .collect(Collectors.joining(", "));
        
        if (!params.isEmpty()) {
            log.debug("Request parameters: [{}]", params);
        }
    }

    /**
     * 응답 상태 추출
     */
    private String getResponseStatus(Object response) {
        if (response instanceof ResponseEntity<?> responseEntity) {
            return String.valueOf(responseEntity.getStatusCode().value());
        }
        return "200";
    }

    /**
     * 응답 데이터 로깅
     */
    private void logResponse(Object response) {
        if (response instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            
            if (body instanceof ApiUtils.ApiResult<?> apiResult) {
                if (apiResult.error() != null) {
                    log.debug("Response error: {}", apiResult.error());
                } else {
                    log.debug("Response data: {}", apiResult.response());
                }
            } else {
                log.debug("Response body: {}", body);
            }
        } else {
            log.debug("Response: {}", response);
        }
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