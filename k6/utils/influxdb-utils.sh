#!/bin/bash

# k6 InfluxDB 환경 관리 공통 유틸리티
# 모든 k6 테스트 스크립트에서 공통으로 사용

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 전역 변수
INFLUXDB_WAS_RUNNING=false
K6_INFLUXDB_URL=${K6_INFLUXDB_URL:-http://host.docker.internal:8086/k6}

# InfluxDB 환경 설정
setup_influxdb() {
    echo "🗄️ InfluxDB 환경 설정 중..."

    # InfluxDB가 이미 실행 중인지 확인
    if docker ps --filter "name=influxdb-k6" --filter "status=running" | grep -q influxdb-k6; then
        echo -e "${GREEN}✅ InfluxDB가 이미 실행 중입니다.${NC}"
        INFLUXDB_WAS_RUNNING=true
        return 0
    fi

    # InfluxDB 시작
    echo "🚀 InfluxDB 시작 중..."
    docker compose -f docker-compose-k6.yml up -d influxdb

    # InfluxDB 준비 대기
    echo "⏳ InfluxDB 준비 대기 중..."
    for i in {1..30}; do
        if curl -s http://localhost:8086/ping >/dev/null 2>&1; then
            echo -e "${GREEN}✅ InfluxDB가 준비되었습니다.${NC}"
            INFLUXDB_WAS_RUNNING=false
            return 0
        fi
        sleep 2
    done

    echo -e "${RED}❌ InfluxDB 시작에 실패했습니다.${NC}"
    return 1
}

# InfluxDB 환경 정리
cleanup_influxdb() {
    if [ "$USE_INFLUXDB" = true ] && [ "$INFLUXDB_WAS_RUNNING" = false ]; then
        echo ""
        echo "🧹 InfluxDB 환경 정리 중..."
        docker compose -f docker-compose-k6.yml down
        echo -e "${GREEN}✅ InfluxDB가 정리되었습니다. (메모리 절약)${NC}"
    fi
}

# 강화된 정리 함수 (백그라운드 실행 대응)
robust_cleanup() {
    local script_name=${1:-"k6"}

    # PID 파일이 있으면 읽고 정리
    if [ -f "/tmp/${script_name}_$$.pid" ]; then
        rm -f "/tmp/${script_name}_$$.pid"
    fi

    # InfluxDB 정리
    cleanup_influxdb

    # 남아있는 k6 프로세스 정리
    pkill -f "k6.*\.js" 2>/dev/null || true
}

# 백그라운드 안전 정리 트랩 설정
setup_cleanup_traps() {
    local script_name=${1:-"k6"}

    # PID 파일 생성
    echo $$ > "/tmp/${script_name}_$$.pid"

    # 모든 종료 시그널에 대해 정리 함수 실행
    trap "robust_cleanup $script_name" EXIT
    trap "robust_cleanup $script_name" INT
    trap "robust_cleanup $script_name" TERM
    trap "robust_cleanup $script_name" HUP
    trap "robust_cleanup $script_name" QUIT

    # 백그라운드 프로세스 종료 시 추가 정리
    if [ -n "$1" ]; then
        # 1초 후 정리 확인 (비동기)
        (sleep 1; check_and_cleanup "$script_name") &
    fi
}

# 정리 상태 확인 및 추가 정리
check_and_cleanup() {
    local script_name=$1

    # PID 파일이 남아있고 프로세스가 없으면 정리
    if [ -f "/tmp/${script_name}_$$.pid" ]; then
        local pid=$(cat "/tmp/${script_name}_$$.pid" 2>/dev/null || echo "")
        if [ -n "$pid" ] && ! kill -0 "$pid" 2>/dev/null; then
            robust_cleanup "$script_name"
        fi
    fi
}

# InfluxDB 상태 확인
check_influxdb_status() {
    if curl -s http://localhost:8086/ping >/dev/null 2>&1; then
        echo -e "${GREEN}✅ InfluxDB가 실행 중입니다.${NC}"
        return 0
    else
        echo -e "${RED}❌ InfluxDB가 실행되지 않고 있습니다.${NC}"
        return 1
    fi
}

# k6 명령어 빌더 (InfluxDB 옵션 포함)
build_k6_command() {
    local test_file=$1
    local test_type=$2
    local result_file=$3

    local k6_cmd="run --summary-export=/results/$result_file"

    if [ "$USE_INFLUXDB" = true ]; then
        local influx_url=${K6_INFLUXDB_URL:-http://host.docker.internal:8086/k6}
        k6_cmd="$k6_cmd --out influxdb=$influx_url"
    fi

    echo "$k6_cmd"
}

# Docker k6 실행 (표준화된 실행)
run_k6_docker() {
    local k6_cmd=$1
    local test_file=$2
    local test_type=$3
    local additional_env=${4:-""}

    # InfluxDB가 필요한 경우 설정
    if [ "$USE_INFLUXDB" = true ]; then
        setup_influxdb
    fi

    local base_env="-e TEST_TYPE=$test_type"
    if [ -n "$K6_BASE_URL" ]; then
        base_env="$base_env -e K6_BASE_URL=$K6_BASE_URL"
    fi
    if [ "$USE_INFLUXDB" = true ]; then
        base_env="$base_env -e K6_INFLUXDB_USERNAME=k6 -e K6_INFLUXDB_PASSWORD=k6 -e K6_INFLUXDB_URL=$K6_INFLUXDB_URL"
    fi

    if [ -n "$additional_env" ]; then
        base_env="$base_env $additional_env"
    fi

    docker run --rm -i \
        --network host \
        -v "$(pwd)/k6:/scripts" \
        -v "$(pwd)/k6/results:/results" \
        $base_env \
        "$K6_IMAGE" $k6_cmd \
        "$test_file"
}

# 스크립트 종료 시 자동 정리 활성화
enable_auto_cleanup() {
    local script_name=$(basename "$0" .sh)
    setup_cleanup_traps "$script_name"
}
