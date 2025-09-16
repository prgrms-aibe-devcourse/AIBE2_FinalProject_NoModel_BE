#!/bin/bash

# AI 모델 검색 성능 테스트 실행 스크립트

set -e

echo "🔍 Starting AI Model Search Performance Testing Suite"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
K6_IMAGE="grafana/k6:latest"
RESULTS_DIR="./k6/results/aimodel"
INFLUXDB_URL="http://localhost:8086"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 도움말 표시
show_help() {
    echo "사용법: $0 [옵션] [테스트_타입]"
    echo ""
    echo "테스트 타입:"
    echo "  smoke     - 스모크 테스트 (기본 검색 기능 검증)"
    echo "  load      - 로드 테스트 (일반적인 검색 부하)"
    echo "  stress    - 스트레스 테스트 (높은 검색 부하)"
    echo "  spike     - 스파이크 테스트 (급격한 부하 변화)"
    echo ""
    echo "옵션:"
    echo "  -h, --help          이 도움말 표시"
    echo "  -i, --influxdb      InfluxDB로 메트릭 전송"
    echo "  -v, --verbose       상세 출력"
    echo "  -r, --results       결과만 표시"
    echo ""
    echo "예시:"
    echo "  $0 smoke"
    echo "  $0 load --influxdb"
    echo "  $0 stress --verbose"
}

# AI 모델 검색 스모크 테스트 실행
run_search_smoke_test() {
    echo -e "${BLUE}📋 AI 모델 검색 스모크 테스트 실행 중...${NC}"

    local k6_cmd="run --summary-export=/results/aimodel/search-smoke-$(date +%Y%m%d_%H%M%S).json"

    if [ "$USE_INFLUXDB" = true ]; then
        k6_cmd="$k6_cmd --out influxdb=$INFLUXDB_URL/k6"
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_INFLUXDB_USERNAME=k6 \
            -e K6_INFLUXDB_PASSWORD=k6 \
            -e TEST_TYPE=smoke \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    else
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e TEST_TYPE=smoke \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    fi
}

# AI 모델 검색 로드 테스트 실행
run_search_load_test() {
    echo -e "${YELLOW}⚡ AI 모델 검색 로드 테스트 실행 중...${NC}"

    local k6_cmd="run --summary-export=/results/aimodel/search-load-$(date +%Y%m%d_%H%M%S).json"

    if [ "$USE_INFLUXDB" = true ]; then
        k6_cmd="$k6_cmd --out influxdb=$INFLUXDB_URL/k6"
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_INFLUXDB_USERNAME=k6 \
            -e K6_INFLUXDB_PASSWORD=k6 \
            -e TEST_TYPE=load \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    else
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e TEST_TYPE=load \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    fi
}

# AI 모델 검색 스트레스 테스트 실행
run_search_stress_test() {
    echo -e "${RED}🔥 AI 모델 검색 스트레스 테스트 실행 중...${NC}"

    local k6_cmd="run --summary-export=/results/aimodel/search-stress-$(date +%Y%m%d_%H%M%S).json"

    if [ "$USE_INFLUXDB" = true ]; then
        k6_cmd="$k6_cmd --out influxdb=$INFLUXDB_URL/k6"
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_INFLUXDB_USERNAME=k6 \
            -e K6_INFLUXDB_PASSWORD=k6 \
            -e TEST_TYPE=stress \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    else
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e TEST_TYPE=stress \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    fi
}

# AI 모델 검색 스파이크 테스트 실행
run_search_spike_test() {
    echo -e "${RED}⚡ AI 모델 검색 스파이크 테스트 실행 중...${NC}"

    local k6_cmd="run --summary-export=/results/aimodel/search-spike-$(date +%Y%m%d_%H%M%S).json"

    if [ "$USE_INFLUXDB" = true ]; then
        k6_cmd="$k6_cmd --out influxdb=$INFLUXDB_URL/k6"
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e K6_INFLUXDB_USERNAME=k6 \
            -e K6_INFLUXDB_PASSWORD=k6 \
            -e TEST_TYPE=spike \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    else
        docker run --rm -i \
            --network host \
            -v "$(pwd)/k6:/scripts" \
            -v "$(pwd)/k6/results:/results" \
            -e TEST_TYPE=spike \
            "$K6_IMAGE" $k6_cmd \
            /scripts/aimodel/search-test.js
    fi
}

# 결과 표시
show_results() {
    echo -e "${GREEN}📊 AI 모델 검색 테스트 결과 요약${NC}"
    echo "결과 파일 위치: $RESULTS_DIR"

    # 최신 결과 파일 찾기
    LATEST_RESULT=$(ls -t "$RESULTS_DIR"/*.json 2>/dev/null | head -1)

    if [ -n "$LATEST_RESULT" ]; then
        echo "최신 결과 파일: $(basename "$LATEST_RESULT")"
        echo ""
        echo "주요 메트릭:"
        if command -v jq >/dev/null 2>&1; then
            # jq가 설치된 경우 JSON 파싱
            echo "- HTTP 요청 지속시간 (95%): $(jq -r '.metrics.http_req_duration.values.p95' "$LATEST_RESULT" 2>/dev/null || echo "N/A")ms"
            echo "- HTTP 요청 실패율: $(jq -r '.metrics.http_req_failed.values.rate' "$LATEST_RESULT" 2>/dev/null || echo "N/A")"
            echo "- 검색 에러율: $(jq -r '.metrics.search_error_rate.values.rate' "$LATEST_RESULT" 2>/dev/null || echo "N/A")"
            echo "- 검색 응답시간 (95%): $(jq -r '.metrics.search_response_time.values.p95' "$LATEST_RESULT" 2>/dev/null || echo "N/A")ms"
        else
            echo "  (jq 설치 후 상세 메트릭 확인 가능)"
        fi
        echo ""
        echo "Grafana 대시보드에서 상세 결과를 확인하세요:"
        echo "http://localhost:3000 (admin/admin123)"
    else
        echo "결과 파일이 없습니다."
    fi
}

# 인수 파싱
USE_INFLUXDB=false
VERBOSE=false
SHOW_RESULTS_ONLY=false
TEST_TYPE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
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
echo "🔍 Spring Boot 애플리케이션 상태 확인 중..."
if ! curl -s http://localhost:8080/api/actuator/health >/dev/null; then
    echo -e "${RED}❌ Spring Boot 애플리케이션이 실행되지 않고 있습니다.${NC}"
    echo "애플리케이션을 먼저 시작해주세요: ./gradlew bootRun"
    exit 1
fi

# AI 모델 검색 엔드포인트 확인
echo "🔍 AI 모델 검색 API 상태 확인 중..."
if ! curl -s http://localhost:8080/api/models/search?page=0&size=1 >/dev/null; then
    echo -e "${YELLOW}⚠️  AI 모델 검색 API에 접근할 수 없습니다.${NC}"
    echo "API가 정상 동작하는지 확인해주세요."
fi

echo -e "${GREEN}✅ Spring Boot 애플리케이션이 실행 중입니다.${NC}"

# InfluxDB 메트릭 출력 설정인 경우 InfluxDB 상태 확인
if [ "$USE_INFLUXDB" = true ]; then
    echo "🔍 InfluxDB 상태 확인 중..."
    if ! curl -s http://localhost:8086/health >/dev/null; then
        echo -e "${YELLOW}⚠️  InfluxDB가 실행되지 않고 있습니다.${NC}"
        echo "InfluxDB 없이 테스트를 계속합니다."
        USE_INFLUXDB=false
    else
        echo -e "${GREEN}✅ InfluxDB가 실행 중입니다.${NC}"
    fi
fi

# 테스트 실행
echo -e "${BLUE}🚀 AI 모델 검색 ${TEST_TYPE} 테스트 시작${NC}"

case $TEST_TYPE in
    smoke)
        run_search_smoke_test
        ;;
    load)
        run_search_load_test
        ;;
    stress)
        run_search_stress_test
        ;;
    spike)
        run_search_spike_test
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
echo -e "${GREEN}🎉 AI 모델 검색 테스트 완료!${NC}"