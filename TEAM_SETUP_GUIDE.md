# NoModel 프로젝트 팀원 Setup 가이드

새로운 팀원이 프로젝트를 정상적으로 실행하기 위한 필수 파일과 설정 가이드입니다.

## 📋 필수 파일 체크리스트

### 1. 프로젝트 코어 파일들
- ✅ **build.gradle** - Gradle 빌드 설정
- ✅ **settings.gradle** - Gradle 프로젝트 설정
- ✅ **gradlew, gradlew.bat** - Gradle Wrapper (실행 가능하게 권한 설정)
- ✅ **CLAUDE.md** - 프로젝트 개발 가이드
- ✅ **.env** - 환경 변수 설정 (민감 정보 포함)

### 2. Spring Boot 설정 파일들
- ✅ **src/main/resources/application.yml** - 메인 설정
- ✅ **src/main/resources/config/application-local.yml** - 로컬 개발 설정
- ✅ **src/main/resources/config/application-docker.yml** - Docker 통합 설정
- ✅ **src/main/resources/config/application-monitoring.yml** - 모니터링 설정
- ✅ **src/main/resources/config/applicaiton-prod.yml** - 운영 설정 (파일명 오타 주의)

### 3. Docker 설정 파일들
- ✅ **compose.yml** - MySQL, Redis 컨테이너
- ✅ **docker-compose-elk.yml** - ELK 스택 (로깅)
- ✅ **docker-compose-monitoring.yml** - Prometheus, Grafana 모니터링

### 4. ELK 스택 설정 파일들
- ✅ **elk/elasticsearch/config/elasticsearch.yml**
- ✅ **elk/logstash/config/logstash.yml**
- ✅ **elk/kibana/config/kibana.yml**
- ✅ **elk/filebeat/config/filebeat.yml**

### 5. VS Code 개발자용 설정 파일들 (자동 생성됨)
- ✅ **.vscode/settings.json** - Java, Gradle 설정
- ✅ **.vscode/launch.json** - 실행/디버그 설정
- ✅ **.vscode/tasks.json** - 빌드 작업 설정
- ✅ **.vscode/extensions.json** - 권장 확장 프로그램

## 🚀 빠른 시작 가이드

### 1. 필수 도구 설치

#### Java 21 설치
```bash
# macOS (Homebrew)
brew install openjdk@21

# Windows (Chocolatey)
choco install openjdk --version=21.0.2

# Ubuntu
sudo apt install openjdk-21-jdk
```

#### Docker & Docker Compose 설치
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치
- Docker Compose는 Docker Desktop에 포함됨

### 2. 프로젝트 Clone 및 설정

```bash
# 1. 프로젝트 클론
git clone <repository-url>
cd NoModel

# 2. Gradle Wrapper 실행 권한 부여 (macOS/Linux)
chmod +x gradlew

# 3. 환경 변수 파일 확인
cat .env  # 설정값 확인

# 4. 의존성 설치 및 빌드 테스트
./gradlew build
```

### 3. IDE별 실행 방법

#### IntelliJ IDEA
1. "Open or Import" → 프로젝트 폴더 선택
2. Gradle 프로젝트로 자동 인식
3. Java 21 설정 확인: File → Project Structure → Project → SDK
4. Run Configuration: NoModelApplication 클래스 실행

#### VS Code
1. 프로젝트 폴더 열기
2. 권장 확장 프로그램 설치 (팝업에서 "Install All" 클릭)
3. F5 키 또는 Run and Debug → "Run NoModel Application" 선택

#### Eclipse
1. Import → Existing Gradle Project
2. Java Build Path에서 JDK 21 설정
3. Run As → Spring Boot App

### 4. 서비스 실행 순서

#### 방법 1: 자동 실행 (권장)
```bash
# Spring Boot 애플리케이션 실행 (Docker 서비스 자동 시작)
./gradlew bootRun
```

#### 방법 2: 수동 실행
```bash
# 1. 기본 서비스 시작 (MySQL, Redis)
docker compose up -d

# 2. ELK 스택 시작 (선택사항)
docker compose -f docker-compose-elk.yml up -d

# 3. 모니터링 스택 시작 (선택사항)
docker compose -f docker-compose-monitoring.yml up -d

# 4. Spring Boot 애플리케이션 실행
./gradlew bootRun
```

## 🔧 설정 값 확인 및 수정

### .env 파일 주요 설정값
```bash
# 서버 포트
SERVER.PORT=8080

# 데이터베이스 설정 (실제 값은 팀 리드에게 문의)
MYSQL_DATABASE=nomodel
MYSQL_USER=[팀에서 전달받은 값 사용]
MYSQL_PASSWORD=[팀에서 전달받은 값 사용]

# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379

# ELK 스택 인증 (실제 값은 팀 리드에게 문의)
ELASTICSEARCH_USERNAME=[팀에서 전달받은 값 사용]
ELASTICSEARCH_PASSWORD=[팀에서 전달받은 값 사용]
```

### 프로필별 실행 방법
```bash
# 로컬 개발 (H2 DB 사용)
./gradlew bootRun --args='--spring.profiles.active=local'

# Docker 환경 (MySQL 사용)
./gradlew bootRun --args='--spring.profiles.active=docker'

# 운영 환경
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 🌐 서비스 접속 URL

### 애플리케이션
- **메인 애플리케이션**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/actuator/health
- **H2 Console** (local 프로필): http://localhost:8080/api/h2-console

### 모니터링 & 로깅
- **Kibana**: http://localhost:5601 (개발환경 - 인증 없음)
- **Elasticsearch**: http://localhost:9200 (개발환경 - 인증 없음)
- **Grafana**: http://localhost:3000 (인증 정보는 팀에서 전달받은 값 사용)
- **Prometheus**: http://localhost:9090

### 데이터베이스
- **MySQL**: localhost:3306 (인증 정보는 팀에서 전달받은 값 사용)
- **Redis**: localhost:6379

## 🧪 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.example.nomodel.NoModelApplicationTests"

# 연속 테스트 (실패해도 계속)
./gradlew test --continue
```

## 🐛 문제 해결

### 1. Java 버전 오류
```bash
# Java 21 설치 확인
java -version
javac -version

# JAVA_HOME 설정 (macOS/Linux)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### 2. 포트 충돌
```bash
# 사용 중인 포트 확인
netstat -tulpn | grep :8080
lsof -i :8080

# Docker 컨테이너 정리
docker compose down
docker system prune -f
```

### 3. Docker 서비스 문제
```bash
# 전체 서비스 상태 확인
docker compose ps
docker compose -f docker-compose-elk.yml ps
docker compose -f docker-compose-monitoring.yml ps

# 로그 확인
docker compose logs -f mysql
docker compose -f docker-compose-elk.yml logs -f elasticsearch
```

### 4. Gradle 빌드 오류
```bash
# Gradle 캐시 정리
./gradlew clean build --refresh-dependencies

# Gradle Wrapper 재다운로드
./gradlew wrapper --gradle-version 8.14.3
```

## 📞 도움말

### 문서 참고
- **CLAUDE.md**: 상세한 개발 가이드
- **MONITORING_SETUP.md**: 모니터링 설정 가이드
- **elk/KIBANA_SETUP.md**: Kibana 대시보드 설정

### 팀 연락처
- 기술적 문제: [팀 리드 연락처]
- 인프라 문제: [DevOps 담당자 연락처]
- 문서 개선: [문서 담당자 연락처]

---

✅ **체크리스트**: 위의 모든 파일과 설정이 완료되면 프로젝트가 정상적으로 실행됩니다.