# 🚀 k6 + Prometheus + Grafana 모니터링 스택

NoModel Spring Boot 애플리케이션을 위한 완전한 성능 모니터링 및 테스트 환경입니다.

## 🏗️ 아키텍처

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   k6 Tests  │───▶│ Prometheus  │───▶│   Grafana   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│Spring Boot  │───▶│   Metrics   │───▶│ Dashboards  │
│Application  │    │ Collection  │    │   & Alerts  │
└─────────────┘    └─────────────┘    └─────────────┘
```

## 🚀 빠른 시작

### 1. 모니터링 스택 실행
```bash
# 모니터링 도구 시작
docker-compose -f docker-compose-monitoring.yml up -d

# 상태 확인
docker-compose -f docker-compose-monitoring.yml ps
```

### 2. 접속 정보
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Spring Boot**: http://localhost:8080

### 3. 성능 테스트 실행
```bash
# 기본 스모크 테스트
./k6/run-tests.sh smoke

# Prometheus와 연동된 로드 테스트
./k6/run-tests.sh load --prometheus

# 스트레스 테스트
./k6/run-tests.sh stress --prometheus
```

## 📊 컴포넌트 상세

### k6 (성능 테스트)
- **포트**: 컨테이너 내부에서 실행
- **테스트 스크립트**: `k6/` 디렉토리
- **결과 저장**: `k6/results/` 디렉토리

**사용 가능한 테스트**:
- `smoke`: 기본 기능 검증 (1명, 1분)
- `load`: 일반 부하 테스트 (10→50명, 5분)
- `stress`: 스트레스 테스트 (최대 300명, 30분)
- `spike`: 스파이크 테스트 (급격한 부하 변화)

### Prometheus (메트릭 수집)
- **포트**: 9090
- **설정**: `monitoring/prometheus/prometheus.yml`
- **수집 간격**: 15초
- **데이터 보존**: 15일

**수집 메트릭**:
- Spring Boot Actuator 메트릭
- JVM 메트릭 (메모리, GC, 스레드)
- HTTP 요청 메트릭
- 데이터베이스 연결 풀 메트릭
- k6 테스트 결과 메트릭

### Grafana (시각화)
- **포트**: 3000
- **계정**: admin/admin123
- **대시보드**: 자동 프로비저닝됨

**포함된 대시보드**:
- **Spring Boot Overview**: 애플리케이션 전반적인 상태
- **k6 Performance**: 성능 테스트 결과
- **Infrastructure**: 시스템 리소스 모니터링

## 🔧 상세 설정

### Spring Boot 메트릭 설정

`application.yml`에 다음 설정 추가:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

### 커스텀 메트릭 추가

```java
@Component
public class CustomMetrics {
    private final Counter orderCounter;
    private final Timer paymentTimer;
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.orderCounter = Counter.builder("orders.created")
            .description("주문 생성 횟수")
            .register(meterRegistry);
            
        this.paymentTimer = Timer.builder("payment.processing.time")
            .description("결제 처리 시간")
            .register(meterRegistry);
    }
}
```

## 📈 대시보드 가이드

### Spring Boot Overview 대시보드
- **HTTP Request Duration**: 응답 시간 분포 (50%, 95%, 99%)
- **JVM Heap Memory**: 힙 메모리 사용률
- **Request Rate by Status**: 상태 코드별 요청률
- **Database Connections**: 활성 DB 연결 수

### k6 Performance 대시보드
- **Virtual Users**: 가상 사용자 수 추이
- **HTTP Request Duration**: k6 테스트 응답 시간
- **Failure Rate**: 요청 실패율
- **Request Rate**: 초당 요청 수

## 🚨 알림 설정

Prometheus에서 다음 알림이 설정됨:
- **높은 응답 시간**: 95% 응답시간 > 500ms
- **높은 에러율**: 에러율 > 5%
- **높은 메모리 사용**: 힙 메모리 > 80%
- **DB 연결 부족**: 연결 풀 사용률 > 80%
- **애플리케이션 다운**: 애플리케이션 응답 없음

## 🛠️ 테스트 시나리오

### 기본 테스트 시나리오
1. **헬스 체크**: `/actuator/health`
2. **사용자 등록**: `POST /api/users/register`
3. **상품 조회**: `GET /api/products`
4. **상품 검색**: `GET /api/products/search`
5. **리포트 조회**: `GET /api/reports/summary`

### 임계값 설정
- **응답 시간**: 95% < 500ms, 99% < 1000ms
- **실패율**: < 5%
- **동시 사용자**: 최대 300명 (스트레스 테스트)

## 📋 운영 가이드

### 일일 모니터링 체크리스트
- [ ] Grafana 대시보드 확인
- [ ] 응답 시간 임계값 체크
- [ ] 에러율 모니터링
- [ ] 메모리 사용률 확인
- [ ] DB 연결 풀 상태 점검

### 정기 성능 테스트
```bash
# 주간 성능 테스트 (자동화 스크립트 예시)
./k6/run-tests.sh smoke --prometheus
./k6/run-tests.sh load --prometheus

# 결과 확인
./k6/run-tests.sh --results
```

### 문제 해결

**k6 테스트 실패 시**:
1. Spring Boot 애플리케이션 상태 확인
2. Docker 컨테이너 로그 확인: `docker logs k6-performance`
3. 네트워크 연결 확인

**Prometheus 메트릭 수집 안됨**:
1. Actuator 엔드포인트 확인: `curl http://localhost:8080/actuator/prometheus`
2. Prometheus 타겟 상태 확인: http://localhost:9090/targets

**Grafana 대시보드 로딩 안됨**:
1. Prometheus 데이터소스 연결 확인
2. 브라우저 캐시 클리어
3. Grafana 컨테이너 재시작

## 🔄 확장 가능성

### 추가 모니터링 도구
- **Jaeger**: 분산 추적
- **AlertManager**: 알림 관리
- **Loki**: 로그 집계
- **Node Exporter**: 시스템 메트릭

### 고급 k6 테스트
- **Browser Testing**: 브라우저 자동화 테스트
- **Protocol Testing**: gRPC, WebSocket 테스트
- **Data-Driven Testing**: CSV/JSON 데이터 활용

이 모니터링 스택으로 NoModel 애플리케이션의 성능을 체계적으로 관리하고 최적화할 수 있습니다!