# ELK Stack vs Prometheus/Grafana 역할 분리 가이드

## 🎯 **핵심 역할 분리**

### **ELK Stack (로깅 & 트레이싱)**
- **목적**: 상세한 로그 분석, 트레이싱, 디버깅, 감사 추적
- **데이터**: 구조화된 JSON 로그, 텍스트 기반 분석
- **사용 시점**: 문제 해결, 상세 분석, 감사, 디버깅

### **Prometheus/Grafana (메트릭 & 모니터링)**
- **목적**: 실시간 메트릭 수집, 알림, 대시보드
- **데이터**: 숫자형 메트릭, 시계열 데이터
- **사용 시점**: 실시간 모니터링, 임계값 알림, 성능 대시보드

---

## 📊 **구체적 역할 분담**

### **1. API 모니터링**

#### **ELK Stack 담당**
```json
{
  "timestamp": "2024-01-01T12:00:00.123",
  "logType": "API",
  "event": "REQUEST_RESPONSE",
  "httpMethod": "POST",
  "requestUri": "/api/users",
  "operation": "UserController.createUser",
  "responseTimeMs": 245,
  "httpStatus": 200,
  "statusCategory": "SUCCESS",
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "traceId": "trace-123456-abc",
  "userId": "user-789",
  "requestInfo": {
    "parameters": {
      "count": 2,
      "details": [...]
    }
  }
}
```
- **용도**: API 호출 상세 분석, 사용자 행동 추적, 에러 디버깅

#### **Prometheus/Grafana 담당**
```prometheus
# 메트릭만 수집
http_requests_total{method="POST", endpoint="/api/users", status="200"}
http_request_duration_seconds{method="POST", endpoint="/api/users"}
```
- **용도**: 실시간 TPS 모니터링, 응답시간 알림, 대시보드

### **2. Service 성능 모니터링**

#### **ELK Stack 담당**
```json
{
  "timestamp": "2024-01-01T12:00:00.123",
  "logType": "PERFORMANCE",
  "component": "SERVICE",
  "operation": "UserService.createUser",
  "executionTimeMs": 1250,
  "status": "SUCCESS",
  "performanceCategory": "WARNING",
  "context": {
    "className": "UserService",
    "methodName": "createUser",
    "parameterCount": 3,
    "success": true
  },
  "traceId": "trace-123456-abc",
  "userId": "user-789"
}
```
- **용도**: 느린 메소드 상세 분석, 파라미터 영향도 분석

#### **Prometheus/Grafana 담당**
```prometheus
# 메트릭만 수집
service_method_execution_seconds{class="UserService", method="createUser", status="success"}
service_method_slow_total{method="UserService.createUser"}
```
- **용도**: 평균 응답시간 추세, 임계값 기반 알림

### **3. 쿼리 분석**

#### **ELK Stack 담당**
```json
{
  "timestamp": "2024-01-01T12:00:00.123",
  "logType": "QUERY_ANALYSIS",
  "queryId": "UserRepository.findByEmail",
  "queryType": "SELECT",
  "executionTimeMs": 1500,
  "severity": "HIGH",
  "performanceCategory": "CRITICAL",
  "queryPattern": "CONDITIONAL_SELECT",
  "optimizationSuggestions": [
    "Check if an index exists on the search column",
    "Consider query optimization or caching"
  ],
  "queryDetails": {
    "success": true,
    "parameterCount": 1,
    "hasParameters": true
  }
}
```
- **용도**: Slow Query 상세 분석, 최적화 제안, 쿼리 패턴 분석

#### **Prometheus/Grafana 담당**
```prometheus
# 메트릭만 수집
repository_query_execution_seconds{query="UserRepository.findByEmail", status="success"}
repository_slow_query_critical_total{query="UserRepository.findByEmail"}
```
- **용도**: 쿼리 성능 추세, 데이터베이스 부하 알림

### **4. 감사 로깅**

#### **ELK Stack 담당**
```json
{
  "timestamp": "2024-01-01T12:00:00.123",
  "logType": "AUDIT",
  "auditId": "AUDIT_1234567890_abc12345",
  "event": "AUDIT_SUCCESS",
  "action": "결제 처리",
  "category": "PAYMENT",
  "level": "HIGH",
  "userId": "user-789",
  "userName": "john.doe@example.com",
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "sessionId": "session-456",
  "details": {
    "executionTimeMs": 350,
    "status": "SUCCESS",
    "performanceCategory": "NORMAL"
  }
}
```
- **용도**: 비즈니스 감사, 컴플라이언스, 사용자 행동 분석

#### **Prometheus/Grafana 담당**
```prometheus
# 메트릭만 수집
audit_count_total{action="결제 처리", category="PAYMENT", level="HIGH", status="SUCCESS"}
audit_execution_seconds{action="결제 처리", category="PAYMENT"}
```
- **용도**: 비즈니스 메트릭 대시보드, 거래량 모니터링

---

## 🔍 **Kibana 검색 최적화 (인프라 레벨)**

### **1. 로그 타입별 동적 인덱싱**
```
- API 로그: nomodel-api-YYYY.MM.dd
- 성능 로그: nomodel-performance-YYYY.MM.dd  
- 쿼리 분석: nomodel-query-YYYY.MM.dd
- 감사 로그: nomodel-audit-YYYY.MM.dd
- 비즈니스 이벤트: nomodel-business-YYYY.MM.dd
- 알림 전용: nomodel-alerts-YYYY.MM.dd
```

### **2. Logstash 자동 분류 및 라우팅**
```yaml
# 구조화 로그 자동 분류
structured_logs (port 5001) → logType 기반 인덱스 분리
standard_logs (port 5000) → 패턴 매칭으로 구조화 데이터 추출
filebeat_logs (port 5044) → 기본 애플리케이션 로그 처리

# 자동 알림 분류
alert_required: true → nomodel-alerts 인덱스로 별도 라우팅
security_event: true → 보안 전용 인덱스 추가 처리
performance_alert: slow_execution → 성능 이슈 자동 태깅
```

### **3. 주요 필터링 필드 (자동 생성)**
```
# Logstash에서 자동 추가되는 메타데이터
- priority: high|medium|low (자동 분류)
- alert_required: true (긴급 처리 필요)
- performance_alert: slow_execution (성능 이슈)
- security_event: true (보안 관련)
- log_source: structured|embedded_structured|filebeat

# 애플리케이션 구조화 데이터
- 시간 범위: @timestamp
- 사용자: userId, userName
- 추적: traceId, spanId
- 성능: performanceCategory, executionTimeMs
- 상태: status, httpStatus, statusCategory
- 심각도: severity, level
- 컴포넌트: component, operation
```

### **3. 대시보드 구성**
```
- API 모니터링: 엔드포인트별 상세 분석
- 에러 추적: 에러 패턴과 스택 트레이스
- 사용자 행동: 사용자별 액션 플로우
- 성능 분석: 느린 구간 상세 분석
- 보안 감사: 중요 액션 및 실패 추적
```

---

## ⚡ **Grafana 대시보드 최적화**

### **1. 실시간 메트릭 대시보드**
```
- API 성능: TPS, 평균 응답시간, 에러율
- 시스템 리소스: CPU, 메모리, 디스크 사용률
- 데이터베이스: 커넥션 풀, 쿼리 성능
- 비즈니스 메트릭: 사용자 등록, 결제 건수
```

### **2. 비즈니스 특화 대시보드 추가**
```yaml
# 새로 추가된 Business Metrics Overview 대시보드
Business Intelligence:
  - Business API Request Rate: 비즈니스 중요 API TPS (Actuator 메트릭)
  - API Response Times: P50/P95 응답시간 (auth, payment, order 등)
  - DB Connection Pool: HikariCP 커넥션 풀 상태 (Actuator)
  - Slow Queries Alert: 커스텀 AOP 감지 느린 쿼리 목록
  - Business Events: 카테고리별 비즈니스 이벤트 분포
  - Security Events Rate: 인증 실패, 패스워드 변경 추이
  - System Health Status: 애플리케이션/DB/Redis 상태
```

### **3. 알림 규칙 (business-alerts.yml)**
```yaml
# 비즈니스 중요 알림
- BusinessAPIHighLatency: P95 > 1초 (2분 연속)
- BusinessAPIErrorRateHigh: 에러율 > 5% (1분 연속)
- AuthenticationFailureSpike: 인증 실패 > 10회/분
- PaymentTransactionFailureHigh: 결제 실패율 > 10%
- DatabaseConnectionPoolExhaustion: 커넥션 풀 사용률 > 80%
- SlowQueryDetected: 쿼리 실행시간 > 2초

# SLA 기반 알림  
- BusinessAPISLABreach: P95 > 2초 또는 에러율 > 2% (5분 연속)
- DatabaseSLABreach: 쿼리 > 5초 또는 커넥션 풀 > 90%
```

---

## 🏗️ **인프라 중심 최적화 접근법**

### **1. 애플리케이션 vs 인프라 역할 분리**

#### **애플리케이션 레벨**
```java
// 비즈니스 특화 로깅만 담당
@Auditable(action = "결제 처리", category = "PAYMENT", level = HIGH)
public String processPayment() { ... }

// 일반 메트릭은 Actuator가 자동 처리
// → Prometheus → Grafana 자동 연계
```
- **역할**: 비즈니스 도메인 특화 정보만 생성
- **장점**: 코드 간소화, 유지보수성 향상, 비즈니스 로직에 집중

#### **인프라 레벨 (Logstash)**
```ruby
# 자동 분류 및 라우팅
if [logType] == "AUDIT" {
  mutate { add_field => { "[@metadata][index_suffix]" => "audit" } }
}

# 자동 알림 태깅
if [level] == "ERROR" or [severity] == "CRITICAL" {
  mutate { add_field => { "alert_required" => "true" } }
}
```
- **역할**: 로그 분류, 라우팅, 인덱싱, 메타데이터 추가
- **장점**: 중앙집중식 관리, 설정 변경 시 애플리케이션 재시작 불필요

### **2. Actuator 자동 메트릭 vs 커스텀 AOP**

#### **Actuator 자동 처리 (제거된 중복 기능)**
```yaml
# application.yml에서 자동 활성화
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```
- **자동 수집**: HTTP 메트릭, JVM 메트릭, 데이터베이스 커넥션 풀
- **Prometheus 연동**: 자동 메트릭 export
- **Grafana 시각화**: 표준 대시보드 활용

#### **커스텀 AOP (비즈니스 특화 유지)**
```java
// 비즈니스 중요 메소드만 선별 모니터링
if (isBusinessCriticalMethod(operation) || executionTime > slowMethodThresholdMs) {
    structuredLogger.logBusinessEvent(log, "SERVICE_EXECUTION", ...);
}
```
- **선별적 로깅**: 비즈니스 중요 로직만 ELK Stack으로 전송
- **상세 분석**: 비즈니스 컨텍스트가 포함된 구조화 로그
- **최적화 제안**: 도메인 특화 최적화 인사이트

### **3. 포트 분리 전략**
```yaml
# 구조화 로그 전용 포트 (StructuredLogger)
TCP 5001: 비즈니스 특화 JSON 로그 → 타입별 인덱스 분리

# 표준 로그 포트
TCP 5000: 일반 애플리케이션 로그 → 패턴 매칭으로 구조화 데이터 추출

# Filebeat 포트
TCP 5044: 파일 기반 로그 수집 → 기본 애플리케이션 로그 처리
```

---

## 🚀 **구현 효과**

### **성능 최적화**
- **ELK Stack**: 구조화된 JSON으로 검색 성능 향상
- **Prometheus**: 메트릭만 수집하여 저장 공간 최적화

### **운영 효율성**
- **실시간 알림**: Prometheus/Grafana
- **상세 분석**: ELK Stack  
- **문제 해결**: 두 시스템 연계를 통한 빠른 원인 파악

### **비용 최적화**
- **중복 데이터 제거**: 메트릭과 로그 분리
- **저장소 효율화**: 용도별 최적화된 저장 전략
- **검색 성능**: 목적에 맞는 도구 활용

이제 두 시스템이 서로의 강점을 살려 **최적의 모니터링 환경**을 제공합니다! 🎉