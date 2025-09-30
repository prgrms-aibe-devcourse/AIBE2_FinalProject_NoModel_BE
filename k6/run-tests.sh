#!/bin/bash

# k6 테스트 실행 스크립트 (통합 시나리오 테스트)

set -e

echo "🚀 Starting k6 Performance Testing Suite"

# 공통 유틸리티 로드
source "$(dirname "$0")/utils/influxdb-utils.sh"

# 기본 설정
K6_IMAGE="grafana/k6:latest"
RESULTS_DIR="./k6/results"
PROMETHEUS_URL="http://localhost:9090/api/v1/write"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 자동 정리 활성화
enable_auto_cleanup

# 도움말 표시
show_help() {
    echo "사용법: $0 [옵션] [테스트_타입]"
    echo ""
    echo "테스트 타입:"
    echo "  smoke     - 스모크 테스트 (기본 기능 검증)"
    echo "  load      - 로드 테스트 (일반적인 부하)"
    echo "  stress    - 스트레스 테스트 (높은 부하)"
    echo "  spike     - 스파이크 테스트 (급격한 부하 변화)"
    echo ""
    echo "도메인별 테스트:"
    echo "  Use k6/aimodel/run-search-tests.sh for AI model search performance tests"
    echo ""
    echo "옵션:"
    echo "  -h, --help          이 도움말 표시"
    echo "  -p, --prometheus    Prometheus로 메트릭 전송"
    echo "  -i, --influxdb      InfluxDB로 메트릭 전송 (자동 환경 관리)"
    echo "  -v, --verbose       상세 출력"
    echo "  -r, --results       결과만 표시"
    echo ""
    echo "예시:"
    echo "  $0 smoke"
    echo "  $0 load --prometheus"
    echo "  $0 load --influxdb      # InfluxDB 자동 시작/종료"
    echo "  $0 stress --verbose"
    echo "  k6/aimodel/run-search-tests.sh load --influxdb"
}

# 스모크 테스트 실행
run_smoke_test() {
    echo -e "${BLUE}📋 스모크 테스트 실행 중...${NC}"

    local result_file="smoke-test-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/scenarios/smoke.js" "smoke" "$result_file")

    if [ "$USE_PROMETHEUS" = true ]; then
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_PROMETHEUS_RW_SERVER_URL="$PROMETHEUS_URL" \
            -e K6_BASE_URL="$K6_BASE_URL" \
            "$K6_IMAGE" run \
            --out experimental-prometheus-rw \
            --summary-export="/results/$result_file" \
            /scripts/scenarios/smoke.js
    else
        run_k6_docker "$k6_cmd" "/scripts/scenarios/smoke.js" "smoke"
    fi
}

# 로드 테스트 실행
run_load_test() {
    echo -e "${YELLOW}⚡ 로드 테스트 실행 중...${NC}"

    local result_file="load-test-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/scenarios/load.js" "load" "$result_file")

    if [ "$USE_PROMETHEUS" = true ]; then
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_PROMETHEUS_RW_SERVER_URL="$PROMETHEUS_URL" \
            -e K6_BASE_URL="$K6_BASE_URL" \
            "$K6_IMAGE" run \
            --out experimental-prometheus-rw \
            --summary-export="/results/$result_file" \
            /scripts/scenarios/load.js
    else
        run_k6_docker "$k6_cmd" "/scripts/scenarios/load.js" "load"
    fi
}

# 스트레스 테스트 실행
run_stress_test() {
    echo -e "${RED}🔥 스트레스 테스트 실행 중...${NC}"

    local result_file="stress-test-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/scenarios/load.js" "stress" "$result_file")

    if [ "$USE_PROMETHEUS" = true ]; then
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_PROMETHEUS_RW_SERVER_URL="$PROMETHEUS_URL" \
            -e K6_BASE_URL="$K6_BASE_URL" \
            "$K6_IMAGE" run \
            --out experimental-prometheus-rw \
            --summary-export="/results/$result_file" \
            -e TEST_TYPE=stress \
            /scripts/scenarios/load.js
    else
        run_k6_docker "$k6_cmd" "/scripts/scenarios/load.js" "stress"
    fi
}

# 스파이크 테스트 실행
run_spike_test() {
    echo -e "${RED}⚡ 스파이크 테스트 실행 중...${NC}"

    local result_file="spike-test-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/scenarios/load.js" "spike" "$result_file")

    if [ "$USE_PROMETHEUS" = true ]; then
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_PROMETHEUS_RW_SERVER_URL="$PROMETHEUS_URL" \
            -e K6_BASE_URL="$K6_BASE_URL" \
            "$K6_IMAGE" run \
            --out experimental-prometheus-rw \
            --summary-export="/results/$result_file" \
            -e TEST_TYPE=spike \
            /scripts/scenarios/load.js
    else
        run_k6_docker "$k6_cmd" "/scripts/scenarios/load.js" "spike"
    fi
}

# 결과 표시
show_results() {
    echo -e "${GREEN}📊 테스트 결과 요약${NC}"
    echo "결과 파일 위치: $RESULTS_DIR"
    
    # 최신 결과 파일 찾기
    LATEST_RESULT=$(ls -t "$RESULTS_DIR"/*.json 2>/dev/null | head -1)
    
    if [ -n "$LATEST_RESULT" ]; then
        echo "최신 결과 파일: $(basename "$LATEST_RESULT")"
        echo ""
        echo "Grafana 대시보드에서 상세 결과를 확인하세요:"
        echo "http://localhost:3000 (admin/admin123)"
    else
        echo "결과 파일이 없습니다."
    fi
}

# 중복 함수 제거 - 공통 유틸리티 사용

# 인수 파싱
USE_PROMETHEUS=false
USE_INFLUXDB=false
VERBOSE=false
SHOW_RESULTS_ONLY=false
TEST_TYPE=""
INFLUXDB_WAS_RUNNING=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -p|--prometheus)
            USE_PROMETHEUS=true
            shift
            ;;
        -i|--influxdb)
            USE_INFLUXDB=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -r|--results)
            SHOW_RESULTS_ONLY=true
            shift
            ;;
        smoke|load|stress|spike)
            TEST_TYPE="$1"
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            show_help
            exit 1
            ;;
    esac
done

# 결과만 표시하고 종료
if [ "$SHOW_RESULTS_ONLY" = true ]; then
    show_results
    exit 0
fi

# 테스트 타입이 지정되지 않은 경우 기본값
if [ -z "$TEST_TYPE" ]; then
    TEST_TYPE="smoke"
fi

# Spring Boot 애플리케이션 실행 확인
K6_BASE_URL=${K6_BASE_URL:-http://host.docker.internal:8080/api}
K6_HEALTH_URL=${K6_HEALTH_URL:-http://localhost:8080/api/actuator/health}

echo "🔍 Spring Boot 애플리케이션 상태 확인 중..."
if ! curl -sf "$K6_HEALTH_URL" >/dev/null; then
    if [ "$K6_HEALTH_URL" = "http://localhost:8080/api/actuator/health" ]; then
        ALT_HEALTH_URL="http://host.docker.internal:8080/api/actuator/health"
        if curl -sf "$ALT_HEALTH_URL" >/dev/null; then
            K6_HEALTH_URL="$ALT_HEALTH_URL"
            K6_BASE_URL="http://host.docker.internal:8080/api"
        else
            echo -e "${RED}❌ Spring Boot 애플리케이션이 실행 중이 아니거나 ${K6_HEALTH_URL} 에서 접근할 수 없습니다.${NC}"
            echo "애플리케이션이 다른 포트/호스트에서 동작 중이면 K6_HEALTH_URL 및 K6_BASE_URL 환경 변수를 지정해 주세요."
            exit 1
        fi
    else
        echo -e "${RED}❌ Spring Boot 애플리케이션이 실행 중이 아니거나 ${K6_HEALTH_URL} 에서 접근할 수 없습니다.${NC}"
        echo "애플리케이션이 다른 포트/호스트에서 동작 중이면 K6_HEALTH_URL 및 K6_BASE_URL 환경 변수를 지정해 주세요."
        exit 1
    fi
fi
echo -e "${GREEN}✅ Spring Boot 애플리케이션이 실행 중입니다. (health check: ${K6_HEALTH_URL})${NC}"

# Prometheus 메트릭 출력 설정인 경우 Prometheus 상태 확인
if [ "$USE_PROMETHEUS" = true ]; then
    echo "🔍 Prometheus 상태 확인 중..."
    if ! curl -s http://localhost:9090/-/healthy >/dev/null; then
        echo -e "${YELLOW}⚠️  Prometheus가 실행되지 않고 있습니다.${NC}"
        echo "Prometheus 없이 테스트를 계속합니다."
        USE_PROMETHEUS=false
    else
        echo -e "${GREEN}✅ Prometheus가 실행 중입니다.${NC}"
    fi
fi

# 테스트 실행
case $TEST_TYPE in
    smoke)
        run_smoke_test
        ;;
    load)
        run_load_test
        ;;
    stress)
        run_stress_test
        ;;
    spike)
        run_spike_test
        ;;
    *)
        echo "지원되지 않는 테스트 타입: $TEST_TYPE"
        show_help
        exit 1
        ;;
esac

# 테스트 완료 후 결과 표시
echo ""
show_results

echo ""
echo -e "${GREEN}🎉 테스트 완료!${NC}"
