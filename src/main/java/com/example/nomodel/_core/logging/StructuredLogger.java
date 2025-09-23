package com.example.nomodel._core.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ELK Stack 최적화를 위한 구조화된 로깅 유틸리티
 * Kibana에서 필터링, 검색, 시각화가 용이한 JSON 구조로 로깅
 */
@Component
@RequiredArgsConstructor
public class StructuredLogger {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /**
     * 성능 로그 (ELK 전용 - 메트릭은 Prometheus에서 처리)
     */
    public void logPerformance(Logger logger, String component, String operation, 
                             long executionTimeMs, String status, Map<String, Object> context) {
        
        PerformanceLog logEntry = PerformanceLog.builder()
            .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
            .logType("PERFORMANCE")
            .component(component)
            .operation(operation)
            .executionTimeMs(executionTimeMs)
            .status(status)
            .traceId(MDC.get("traceId"))
            .spanId(MDC.get("spanId"))
            .userId(MDC.get("userId"))
            .sessionId(MDC.get("sessionId"))
            .context(context)
            .build();

        // 성능 임계값에 따른 로그 레벨 결정 (ELK에서 필터링 용이)
        if ("ERROR".equals(status)) {
            logger.error("PERF_LOG {}", toJson(logEntry));
        } else if (executionTimeMs > 1000) {
            logger.warn("PERF_LOG {}", toJson(logEntry));
        } else {
            logger.info("PERF_LOG {}", toJson(logEntry));
        }
    }

    /**
     * 감사 로그 (ELK 전용 - 상세 추적을 위한 구조화)
     */
    public void logAudit(Logger logger, String event, String action, String category,
                        String level, String userId, String userName, Map<String, Object> details) {
        
        AuditLog logEntry = AuditLog.builder()
            .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
            .logType("AUDIT")
            .auditId(generateAuditId())
            .event(event)
            .action(action)
            .category(category)
            .level(level)
            .userId(userId)
            .userName(userName)
            .clientIp(MDC.get("clientIp"))
            .userAgent(MDC.get("userAgent"))
            .sessionId(MDC.get("sessionId"))
            .traceId(MDC.get("traceId"))
            .details(details)
            .build();

        // 중요도에 따른 로그 레벨
        switch (level) {
            case "HIGH" -> logger.warn("AUDIT_LOG {}", toJson(logEntry));
            case "MEDIUM" -> logger.info("AUDIT_LOG {}", toJson(logEntry));
            case "LOW" -> logger.debug("AUDIT_LOG {}", toJson(logEntry));
            default -> logger.info("AUDIT_LOG {}", toJson(logEntry));
        }
    }

    /**
     * 쿼리 분석 로그 (ELK 전용 - Slow Query 상세 분석)
     */
    public void logQueryAnalysis(Logger logger, String queryId, String queryType, 
                               long executionTimeMs, Map<String, Object> queryDetails) {
        
        QueryAnalysisLog logEntry = QueryAnalysisLog.builder()
            .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
            .logType("QUERY_ANALYSIS")
            .queryId(queryId)
            .queryType(queryType)
            .executionTimeMs(executionTimeMs)
            .severity(determineSeverity(executionTimeMs))
            .traceId(MDC.get("traceId"))
            .userId(MDC.get("userId"))
            .queryDetails(queryDetails)
            .build();

        // 쿼리 성능에 따른 로그 레벨
        if (executionTimeMs > 3000) {
            logger.error("QUERY_LOG {}", toJson(logEntry));
        } else if (executionTimeMs > 1000) {
            logger.warn("QUERY_LOG {}", toJson(logEntry));
        } else {
            logger.debug("QUERY_LOG {}", toJson(logEntry));
        }
    }

    /**
     * API 요청/응답 로그 (ELK 전용)
     */
    public void logApiRequest(Logger logger, String method, String uri, String operation,
                             long responseTimeMs, int statusCode, Map<String, Object> requestInfo) {
        
        ApiLog logEntry = ApiLog.builder()
            .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
            .logType("API")
            .event("REQUEST_RESPONSE")
            .httpMethod(method)
            .requestUri(uri)
            .operation(operation)
            .responseTimeMs(responseTimeMs)
            .httpStatus(statusCode)
            .statusCategory(getStatusCategory(statusCode))
            .clientIp(MDC.get("clientIp"))
            .userAgent(MDC.get("userAgent"))
            .traceId(MDC.get("traceId"))
            .userId(MDC.get("userId"))
            .sessionId(MDC.get("sessionId"))
            .requestInfo(requestInfo)
            .build();

        // HTTP 상태코드에 따른 로그 레벨
        if (statusCode >= 500) {
            logger.error("API_LOG {}", toJson(logEntry));
        } else if (statusCode >= 400) {
            logger.warn("API_LOG {}", toJson(logEntry));
        } else {
            logger.info("API_LOG {}", toJson(logEntry));
        }
    }

    /**
     * 비즈니스 이벤트 로그 (ELK 전용)
     */
    public void logBusinessEvent(Logger logger, String eventType, String description, 
                               String severity, Map<String, Object> eventData) {
        
        BusinessEventLog logEntry = BusinessEventLog.builder()
            .timestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
            .logType("BUSINESS_EVENT")
            .eventType(eventType)
            .description(description)
            .severity(severity)
            .traceId(MDC.get("traceId"))
            .userId(MDC.get("userId"))
            .sessionId(MDC.get("sessionId"))
            .eventData(eventData)
            .build();

        switch (severity) {
            case "CRITICAL" -> logger.info("BUSINESS_LOG {}", toJson(logEntry));
            case "HIGH" -> logger.info("BUSINESS_LOG {}", toJson(logEntry));
            case "MEDIUM" -> logger.info("BUSINESS_LOG {}", toJson(logEntry));
            case "LOW" -> logger.debug("BUSINESS_LOG {}", toJson(logEntry));
            default -> logger.info("BUSINESS_LOG {}", toJson(logEntry));
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "JSON_SERIALIZATION_ERROR: " + e.getMessage();
        }
    }

    private String generateAuditId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String determineSeverity(long executionTimeMs) {
        if (executionTimeMs > 3000) return "CRITICAL";
        if (executionTimeMs > 1000) return "HIGH";
        if (executionTimeMs > 500) return "MEDIUM";
        return "LOW";
    }

    private String getStatusCategory(int statusCode) {
        if (statusCode >= 500) return "SERVER_ERROR";
        if (statusCode >= 400) return "CLIENT_ERROR";
        if (statusCode >= 300) return "REDIRECT";
        if (statusCode >= 200) return "SUCCESS";
        return "INFORMATIONAL";
    }

    // 로그 엔트리 클래스들
    public static class PerformanceLog {
        public String timestamp;
        public String logType;
        public String component;
        public String operation;
        public long executionTimeMs;
        public String status;
        public String traceId;
        public String spanId;
        public String userId;
        public String sessionId;
        public Map<String, Object> context;

        public static PerformanceLogBuilder builder() {
            return new PerformanceLogBuilder();
        }

        public static class PerformanceLogBuilder {
            private PerformanceLog log = new PerformanceLog();
            public PerformanceLogBuilder timestamp(String timestamp) { log.timestamp = timestamp; return this; }
            public PerformanceLogBuilder logType(String logType) { log.logType = logType; return this; }
            public PerformanceLogBuilder component(String component) { log.component = component; return this; }
            public PerformanceLogBuilder operation(String operation) { log.operation = operation; return this; }
            public PerformanceLogBuilder executionTimeMs(long executionTimeMs) { log.executionTimeMs = executionTimeMs; return this; }
            public PerformanceLogBuilder status(String status) { log.status = status; return this; }
            public PerformanceLogBuilder traceId(String traceId) { log.traceId = traceId; return this; }
            public PerformanceLogBuilder spanId(String spanId) { log.spanId = spanId; return this; }
            public PerformanceLogBuilder userId(String userId) { log.userId = userId; return this; }
            public PerformanceLogBuilder sessionId(String sessionId) { log.sessionId = sessionId; return this; }
            public PerformanceLogBuilder context(Map<String, Object> context) { log.context = context; return this; }
            public PerformanceLog build() { return log; }
        }
    }

    public static class AuditLog {
        public String timestamp;
        public String logType;
        public String auditId;
        public String event;
        public String action;
        public String category;
        public String level;
        public String userId;
        public String userName;
        public String clientIp;
        public String userAgent;
        public String sessionId;
        public String traceId;
        public Map<String, Object> details;

        public static AuditLogBuilder builder() {
            return new AuditLogBuilder();
        }

        public static class AuditLogBuilder {
            private AuditLog log = new AuditLog();
            public AuditLogBuilder timestamp(String timestamp) { log.timestamp = timestamp; return this; }
            public AuditLogBuilder logType(String logType) { log.logType = logType; return this; }
            public AuditLogBuilder auditId(String auditId) { log.auditId = auditId; return this; }
            public AuditLogBuilder event(String event) { log.event = event; return this; }
            public AuditLogBuilder action(String action) { log.action = action; return this; }
            public AuditLogBuilder category(String category) { log.category = category; return this; }
            public AuditLogBuilder level(String level) { log.level = level; return this; }
            public AuditLogBuilder userId(String userId) { log.userId = userId; return this; }
            public AuditLogBuilder userName(String userName) { log.userName = userName; return this; }
            public AuditLogBuilder clientIp(String clientIp) { log.clientIp = clientIp; return this; }
            public AuditLogBuilder userAgent(String userAgent) { log.userAgent = userAgent; return this; }
            public AuditLogBuilder sessionId(String sessionId) { log.sessionId = sessionId; return this; }
            public AuditLogBuilder traceId(String traceId) { log.traceId = traceId; return this; }
            public AuditLogBuilder details(Map<String, Object> details) { log.details = details; return this; }
            public AuditLog build() { return log; }
        }
    }

    public static class QueryAnalysisLog {
        public String timestamp;
        public String logType;
        public String queryId;
        public String queryType;
        public long executionTimeMs;
        public String severity;
        public String traceId;
        public String userId;
        public Map<String, Object> queryDetails;

        public static QueryAnalysisLogBuilder builder() {
            return new QueryAnalysisLogBuilder();
        }

        public static class QueryAnalysisLogBuilder {
            private QueryAnalysisLog log = new QueryAnalysisLog();
            public QueryAnalysisLogBuilder timestamp(String timestamp) { log.timestamp = timestamp; return this; }
            public QueryAnalysisLogBuilder logType(String logType) { log.logType = logType; return this; }
            public QueryAnalysisLogBuilder queryId(String queryId) { log.queryId = queryId; return this; }
            public QueryAnalysisLogBuilder queryType(String queryType) { log.queryType = queryType; return this; }
            public QueryAnalysisLogBuilder executionTimeMs(long executionTimeMs) { log.executionTimeMs = executionTimeMs; return this; }
            public QueryAnalysisLogBuilder severity(String severity) { log.severity = severity; return this; }
            public QueryAnalysisLogBuilder traceId(String traceId) { log.traceId = traceId; return this; }
            public QueryAnalysisLogBuilder userId(String userId) { log.userId = userId; return this; }
            public QueryAnalysisLogBuilder queryDetails(Map<String, Object> queryDetails) { log.queryDetails = queryDetails; return this; }
            public QueryAnalysisLog build() { return log; }
        }
    }

    public static class ApiLog {
        public String timestamp;
        public String logType;
        public String event;
        public String httpMethod;
        public String requestUri;
        public String operation;
        public long responseTimeMs;
        public int httpStatus;
        public String statusCategory;
        public String clientIp;
        public String userAgent;
        public String traceId;
        public String userId;
        public String sessionId;
        public Map<String, Object> requestInfo;

        public static ApiLogBuilder builder() {
            return new ApiLogBuilder();
        }

        public static class ApiLogBuilder {
            private ApiLog log = new ApiLog();
            public ApiLogBuilder timestamp(String timestamp) { log.timestamp = timestamp; return this; }
            public ApiLogBuilder logType(String logType) { log.logType = logType; return this; }
            public ApiLogBuilder event(String event) { log.event = event; return this; }
            public ApiLogBuilder httpMethod(String httpMethod) { log.httpMethod = httpMethod; return this; }
            public ApiLogBuilder requestUri(String requestUri) { log.requestUri = requestUri; return this; }
            public ApiLogBuilder operation(String operation) { log.operation = operation; return this; }
            public ApiLogBuilder responseTimeMs(long responseTimeMs) { log.responseTimeMs = responseTimeMs; return this; }
            public ApiLogBuilder httpStatus(int httpStatus) { log.httpStatus = httpStatus; return this; }
            public ApiLogBuilder statusCategory(String statusCategory) { log.statusCategory = statusCategory; return this; }
            public ApiLogBuilder clientIp(String clientIp) { log.clientIp = clientIp; return this; }
            public ApiLogBuilder userAgent(String userAgent) { log.userAgent = userAgent; return this; }
            public ApiLogBuilder traceId(String traceId) { log.traceId = traceId; return this; }
            public ApiLogBuilder userId(String userId) { log.userId = userId; return this; }
            public ApiLogBuilder sessionId(String sessionId) { log.sessionId = sessionId; return this; }
            public ApiLogBuilder requestInfo(Map<String, Object> requestInfo) { log.requestInfo = requestInfo; return this; }
            public ApiLog build() { return log; }
        }
    }

    public static class BusinessEventLog {
        public String timestamp;
        public String logType;
        public String eventType;
        public String description;
        public String severity;
        public String traceId;
        public String userId;
        public String sessionId;
        public Map<String, Object> eventData;

        public static BusinessEventLogBuilder builder() {
            return new BusinessEventLogBuilder();
        }

        public static class BusinessEventLogBuilder {
            private BusinessEventLog log = new BusinessEventLog();
            public BusinessEventLogBuilder timestamp(String timestamp) { log.timestamp = timestamp; return this; }
            public BusinessEventLogBuilder logType(String logType) { log.logType = logType; return this; }
            public BusinessEventLogBuilder eventType(String eventType) { log.eventType = eventType; return this; }
            public BusinessEventLogBuilder description(String description) { log.description = description; return this; }
            public BusinessEventLogBuilder severity(String severity) { log.severity = severity; return this; }
            public BusinessEventLogBuilder traceId(String traceId) { log.traceId = traceId; return this; }
            public BusinessEventLogBuilder userId(String userId) { log.userId = userId; return this; }
            public BusinessEventLogBuilder sessionId(String sessionId) { log.sessionId = sessionId; return this; }
            public BusinessEventLogBuilder eventData(Map<String, Object> eventData) { log.eventData = eventData; return this; }
            public BusinessEventLog build() { return log; }
        }
    }
}