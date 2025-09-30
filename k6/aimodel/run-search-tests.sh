#!/bin/bash

# AI 모델 검색 성능 테스트 실행 스크립트

set -e

echo "🔍 Starting AI Model Search Performance Testing Suite"

# 공통 유틸리티 로드
source "$(dirname "$0")/../utils/influxdb-utils.sh"

# 기본 설정
K6_IMAGE="grafana/k6:latest"
RESULTS_DIR="./k6/results/aimodel"
INFLUXDB_URL="http://localhost:8086"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 자동 정리 활성화
enable_auto_cleanup

# 도움말 표시
show_help() {
    echo "사용법: $0 [옵션] [테스트_타입]"
    echo ""
    echo "테스트 타입:"
    echo "  smoke     - 스모크 테스트 (기본 검색 기능 검증)"
    echo "  load      - 로드 테스트 (일반적인 검색 부하)"
    echo "  load-short - 짧은 로드 테스트 (2분)"
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

    local result_file="search-smoke-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/aimodel/search-test.js" "smoke" "aimodel/$result_file")

    run_k6_docker "$k6_cmd" "/scripts/aimodel/search-test.js" "smoke"
}

# AI 모델 검색 로드 테스트 실행
run_search_load_test() {
    echo -e "${YELLOW}⚡ AI 모델 검색 로드 테스트 실행 중...${NC}"

    local result_file="search-load-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/aimodel/search-test.js" "load" "aimodel/$result_file")

    run_k6_docker "$k6_cmd" "/scripts/aimodel/search-test.js" "load"
}

# AI 모델 검색 짧은 로드 테스트 실행
run_search_load_short_test() {
    echo -e "${YELLOW}⚡ AI 모델 검색 짧은 로드 테스트 실행 중...${NC}"

    local result_file="search-load-short-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/aimodel/search-test.js" "load-short" "aimodel/$result_file")

    run_k6_docker "$k6_cmd" "/scripts/aimodel/search-test.js" "load-short"
}

# AI 모델 검색 스트레스 테스트 실행
run_search_stress_test() {
    echo -e "${RED}🔥 AI 모델 검색 스트레스 테스트 실행 중...${NC}"

    local result_file="search-stress-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/aimodel/search-test.js" "stress" "aimodel/$result_file")

    run_k6_docker "$k6_cmd" "/scripts/aimodel/search-test.js" "stress"
}

# AI 모델 검색 스파이크 테스트 실행
run_search_spike_test() {
    echo -e "${RED}⚡ AI 모델 검색 스파이크 테스트 실행 중...${NC}"

    local result_file="search-spike-$(date +%Y%m%d_%H%M%S).json"
    local k6_cmd=$(build_k6_command "/scripts/aimodel/search-test.js" "spike" "aimodel/$result_file")

    run_k6_docker "$k6_cmd" "/scripts/aimodel/search-test.js" "spike"
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
            echo "- HTTP 요청 지속시간 (95%): $(jq -r '.metrics.http_req_duration.values."p(95)"' "$LATEST_RESULT" 2>/dev/null || echo "N/A")ms"
            echo "- HTTP 요청 실패율: $(jq -r '.metrics.http_req_failed.values.rate' "$LATEST_RESULT" 2>/dev/null || echo "N/A")"
            echo "- 검색 에러율: $(jq -r '.metrics.search_error_rate.values.rate' "$LATEST_RESULT" 2>/dev/null || echo "N/A")"
            echo "- 검색 응답시간 (95%): $(jq -r '.metrics.search_response_time.values."p(95)"' "$LATEST_RESULT" 2>/dev/null || echo "N/A")ms"
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
        smoke|load|load-short|stress|spike)
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
    if ! check_influxdb_status; then
        echo -e "${YELLOW}⚠️  InfluxDB 자동 시작을 진행합니다.${NC}"
        setup_influxdb
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
    load-short)
        run_search_load_short_test
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
