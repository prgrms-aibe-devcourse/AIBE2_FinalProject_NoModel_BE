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

## 🔍 **Kibana 검색 최적화**

### **1. 로그 타입별 인덱스 패턴**
```
- API 로그: logType:API
- 성능 로그: logType:PERFORMANCE  
- 쿼리 분석: logType:QUERY_ANALYSIS
- 감사 로그: logType:AUDIT
- 비즈니스 이벤트: logType:BUSINESS_EVENT
```

### **2. 주요 필터링 필드**
```
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

### **2. 알림 규칙**
```
- API 응답시간 > 1초 (5분 연속)
- 에러율 > 5% (2분 연속)  
- Slow Query > 10회/분
- 중요 비즈니스 액션 실패 > 3회/분
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