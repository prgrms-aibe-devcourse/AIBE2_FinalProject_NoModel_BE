# k6 Performance Testing

## Quick Start

### 1. 성능 테스트 인프라 시작
```bash
# InfluxDB + k6 환경 시작 (성능 테스트 시에만)
docker compose -f docker-compose-k6.yml up -d influxdb

# InfluxDB 준비 확인
curl http://localhost:8086/ping
```

### 2. 성능 테스트 실행
```bash
# 기본 로드 테스트
./k6/run-tests.sh load --influxdb

# 스트레스 테스트
./k6/run-tests.sh stress --influxdb

# 스모크 테스트
./k6/run-tests.sh smoke --influxdb
```

### 3. 테스트 완료 후 정리
```bash
# InfluxDB 중지 및 제거 (메모리 절약)
docker compose -f docker-compose-k6.yml down

# 또는 InfluxDB만 중지
docker stop influxdb-k6
```

## Architecture

### 평상시 (Production)
```
Spring Boot App + MySQL + Redis + Elasticsearch
+ Monitoring Stack (Prometheus + Grafana + Loki)
= ~2.2GB RAM
```

### 성능 테스트 시
```
기본 스택 + InfluxDB + k6
= ~2.4GB RAM
```

## Benefits

- **메모리 절약**: 평상시 148MB 절약
- **온디맨드**: 필요할 때만 성능 테스트 인프라 실행
- **깔끔한 분리**: 모니터링과 성능 테스트 인프라 분리