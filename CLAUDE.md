# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a comprehensive Spring Boot 3.5.4 application built with Java 21 and Gradle. The project implements a modular monolith architecture using Spring Modulith with extensive observability, monitoring, and performance testing capabilities.

**Authentication Flow**:
- JWT-based authentication using `JWTTokenProvider` with access/refresh token pattern
- Redis-based first login detection system (`FirstLoginRedisRepository`)
- Custom `UserDetails` implementation with member ID for business logic
- OAuth2 client support for social login integration

### Key Technologies
- **Spring Boot 3.5.4** with Java 21 (Gradle 8.14.3)
- **Spring Modulith 1.4.1** for modular monolith architecture
- **Spring Security + OAuth2** client support
- **Spring Data JPA** with MySQL/H2 databases
- **Spring Data Redis** for caching and messaging
- **Apache Kafka** for event streaming
- **Spring Batch** for batch processing
- **SpringDoc OpenAPI** for API documentation
- **Logback + Logstash** for structured logging

### Observability Stack
- **ELK Stack**: Elasticsearch 8.15.0, Logstash 8.15.0, Kibana 8.15.0, Filebeat 8.15.0
- **Prometheus + Grafana**: Metrics collection and visualization
- **k6**: Performance testing and load testing
- **Node Exporter**: System metrics monitoring
- **Micrometer**: Application metrics with Prometheus registry

## Development Commands

### Application Lifecycle
```bash
# Run the application (starts with Docker Compose services automatically)
./gradlew bootRun

# Run with specific profiles
./gradlew bootRun --args='--spring.profiles.active=local'

# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Build Docker image
./gradlew bootBuildImage

# Generate API documentation
./gradlew asciidoctor
```

### Testing Commands
```bash
# Run all tests
./gradlew test

# Run tests with specific profile
./gradlew test --args='--spring.profiles.active=local'

# Run specific test class
./gradlew test --tests "com.example.nomodel.NoModelApplicationTests"

# Run specific test method
./gradlew test --tests "com.example.nomodel.NoModelApplicationTests.contextLoads"

# Run tests matching pattern
./gradlew test --tests "*Integration*"

# Run tests with generated test reports
./gradlew test --continue

# Test runtime classpath
./gradlew bootTestRun

# Integration tests only
./gradlew integrationTest

# Skip tests during build
./gradlew build -x test
```

### Docker Compose Services

#### Core Services (Database & Cache)
```bash
# Start MySQL and Redis (automatically started with bootRun)
docker compose up -d

# Stop core services
docker compose down

# View logs
docker compose logs -f [mysql|redis]
```

#### ELK Stack (Logging & Observability)
```bash
# Start complete ELK stack
docker compose -f docker-compose-elk.yml up -d

# Stop ELK stack
docker compose -f docker-compose-elk.yml down

# View specific service logs
docker compose -f docker-compose-elk.yml logs -f [elasticsearch|logstash|kibana|filebeat]

# Check ELK services health
docker compose -f docker-compose-elk.yml ps
```

#### Monitoring Stack (Prometheus + Grafana + k6)
```bash
# Start monitoring infrastructure
docker compose -f docker-compose-monitoring.yml up -d

# Stop monitoring services
docker compose -f docker-compose-monitoring.yml down

# Run performance tests
./k6/run-tests.sh smoke
./k6/run-tests.sh load --prometheus
./k6/run-tests.sh stress --prometheus
```

### Service Access Points

#### Application Services
- **Spring Boot App**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Actuator Health**: http://localhost:8080/api/actuator/health
- **Actuator Metrics**: http://localhost:8080/api/actuator/metrics
- **H2 Console**: http://localhost:8080/api/h2-console (local profile)

#### ELK Stack
- **Kibana UI**: http://localhost:5601 (elastic/elastic)
- **Elasticsearch**: http://localhost:9200 (elastic/elastic)
- **Logstash API**: http://localhost:9600

#### Monitoring Stack
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Node Exporter**: http://localhost:9100

#### Database Services
- **MySQL**: localhost:3306 (nomodel/nomodel)
- **Redis**: localhost:6379

## Architecture

### Spring Modulith Structure
The application follows Domain-Driven Design principles with Spring Modulith:

```
src/main/java/com/example/nomodel/
├── NoModelApplication.java           # Main application class
├── _core/                           # Core infrastructure module
│   ├── common/                      # Base entities and common classes
│   │   ├── BaseEntity.java         # JPA base entity with ID
│   │   └── BaseTimeEntity.java     # Auditing base entity
│   ├── config/                     # Configuration classes
│   │   ├── AuditConfig.java        # JPA auditing configuration
│   │   ├── AuditorAwareImpl.java   # Current auditor implementation
│   │   ├── SecurityConfig.java     # Spring Security configuration
│   │   └── SwaggerConfig.java      # OpenAPI documentation config
│   ├── controller/                 # API controllers
│   │   └── TestApiController.java  # Demo API with Swagger annotations
│   ├── exception/                  # Exception handling
│   │   ├── ApplicationException.java
│   │   ├── ErrorCode.java
│   │   └── GlobalControllerAdvice.java
│   ├── security/                   # Security implementation
│   │   ├── CustomUserDetails.java      # Custom UserDetails with member ID
│   │   └── CustomUserDetailsService.java # Authentication service
│   └── utils/                      # Utility classes
│       └── ApiUtils.java
└── member/                          # Member domain module
    └── domain/
        ├── model/
        │   ├── Member.java          # Member entity with embedded Email/Password
        │   ├── Role.java            # Role enum (USER, ADMIN)
        │   ├── Status.java          # Member status enum (ACTIVE, SUSPENDED)
        │   ├── Email.java           # Email value object
        │   └── Password.java        # Password value object
        └── repository/
            └── MemberJpaRepository.java # Member repository with email lookup
```

### Configuration Architecture

#### Profile-Based Configuration
- **`application.yml`**: Base configuration with prod profile
- **`application-local.yml`**: H2 database, development settings
- **`application-prod.yml`**: MySQL database, production settings
- **`application-docker.yml`**: Docker Compose integration settings
- **`application-monitoring.yml`**: Actuator and observability settings

#### Logging Configuration
- **`logback-spring.xml`**: Profile-specific logging configuration
- **Console logging**: Development and non-prod profiles
- **File logging**: Structured file outputs for dev profile
- **Logstash logging**: JSON-formatted logs to ELK stack
- **Profile-specific log levels**: DEBUG for development, INFO for production

### Key Dependencies & Integrations

#### Core Spring Boot Stack
- **spring-boot-starter-web**: REST API endpoints
- **spring-boot-starter-validation**: Bean validation
- **spring-boot-starter-actuator**: Health checks and metrics
- **spring-boot-starter-security**: Authentication and authorization
- **spring-boot-starter-oauth2-client**: OAuth2 integration
- **jjwt**: JWT token processing (API, implementation, Jackson integration)

#### Data Access Layer
- **spring-boot-starter-data-jpa**: JPA with Hibernate
- **spring-boot-starter-data-redis**: Redis integration
- **mysql-connector-j**: MySQL driver
- **h2database**: In-memory database for testing

#### Messaging & Events
- **spring-kafka**: Kafka integration
- **spring-boot-starter-batch**: Batch processing
- **spring-boot-starter-mail**: Email capabilities

#### Spring Modulith Components
- **spring-modulith-starter-core**: Core modular architecture
- **spring-modulith-starter-jpa**: JPA integration for modules
- **spring-modulith-events-api**: Event-driven communication
- **spring-modulith-events-kafka**: Kafka event publishing (runtime)
- **spring-modulith-actuator**: Module health monitoring (runtime)
- **spring-modulith-observability**: Distributed tracing (runtime)

#### Documentation & Development
- **springdoc-openapi-starter-webmvc-ui**: Swagger/OpenAPI 3
- **spring-restdocs-mockmvc**: API documentation generation
- **spring-boot-devtools**: Hot reloading
- **spring-boot-docker-compose**: Automatic service startup
- **lombok**: Code generation

#### Observability & Monitoring
- **logstash-logback-encoder**: Structured JSON logging
- **micrometer-registry-prometheus**: Prometheus metrics export
- **spring-modulith-starter-test**: Module testing support

#### Testing Framework
- **spring-boot-starter-test**: JUnit 5, Mockito, TestContainers
- **spring-security-test**: Security testing utilities
- **spring-batch-test**: Batch job testing
- **spring-kafka-test**: Kafka testing utilities

## ELK Stack Integration

### Logging Pipeline
```
Spring Boot App → Logstash (TCP:5001) → Elasticsearch → Kibana
              ↘ File Logs → Filebeat → Logstash → Elasticsearch
```

### Log Structure
The application generates structured JSON logs with the following fields:
- **Standard fields**: @timestamp, message, level, logger_name, thread_name
- **Application fields**: application, environment, service, version
- **Tracing fields**: trace_id, span_id, transaction_id
- **HTTP fields**: http.method, http.url, http.status_code, http.response_time
- **Exception fields**: exception.class, exception.message, stack_trace

### Kibana Setup
1. Start ELK stack: `docker compose -f docker-compose-elk.yml up -d`
2. Access Kibana: http://localhost:5601 (elastic/elastic)
3. Create index pattern: `nomodel-logs-*` with time field `@timestamp`
4. Import dashboards from `elk/kibana/dashboards/`
5. See `elk/KIBANA_SETUP.md` for detailed configuration

## Performance Testing & Monitoring

### k6 Performance Testing
```bash
# Available test scenarios
./k6/run-tests.sh smoke      # Basic functionality (1 user, 1 minute)
./k6/run-tests.sh load       # Load testing (10→50 users, 5 minutes)
./k6/run-tests.sh stress     # Stress testing (up to 300 users, 30 minutes)
./k6/run-tests.sh spike      # Spike testing (sudden load changes)

# With Prometheus integration
./k6/run-tests.sh load --prometheus
```

### Monitoring Stack
- **Prometheus**: Metrics collection (15-second intervals, 15-day retention)
- **Grafana**: Pre-configured dashboards for Spring Boot, k6, and infrastructure
- **Node Exporter**: System-level metrics (CPU, memory, disk, network)
- **Alert Rules**: Response time, error rate, memory usage, and availability alerts

### Available Dashboards
- **Spring Boot Overview**: Application health, HTTP metrics, JVM statistics
- **k6 Performance**: Test results, virtual users, response times, failure rates
- **Infrastructure**: System resources, Docker container metrics

## Development Workflow

### Recommended Development Setup
1. **Start infrastructure**: `docker compose up -d` (MySQL, Redis)
2. **Start ELK stack**: `docker compose -f docker-compose-elk.yml up -d`
3. **Run application**: `./gradlew bootRun`
4. **Run tests**: `./gradlew test`
5. **Optional monitoring**: `docker compose -f docker-compose-monitoring.yml up -d`

### Environment Variables
Create `.env` file in project root for environment-specific configuration:
```bash
# Database
MYSQL_DATABASE=nomodel
MYSQL_USER=nomodel
MYSQL_PASSWORD=nomodel
MYSQL_ROOT_PASSWORD=root123
MYSQL_PORT=3306

# Redis
REDIS_PASSWORD=
REDIS_PORT=6379

# ELK Stack
ELASTIC_PASSWORD=elastic
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=elastic
KIBANA_SYSTEM_USERNAME=elastic
KIBANA_SYSTEM_PASSWORD=elastic
```

### Testing Strategy
- **Unit Tests**: JUnit 5 with Mockito for individual components
- **Integration Tests**: TestContainers for database integration
- **Security Tests**: Spring Security Test for authentication/authorization
- **Module Tests**: Spring Modulith test support for module boundaries
- **Performance Tests**: k6 scripts for load and stress testing
- **API Tests**: Spring REST Docs for documentation and contract testing

## Troubleshooting

### Common Issues
1. **Port conflicts**: Check Docker port mappings and local service conflicts
2. **Database connection**: Verify MySQL container is running and credentials
3. **ELK stack startup**: Services have dependencies; allow time for complete startup
4. **Log ingestion**: Verify Logstash TCP port 5001 is accessible
5. **Metrics collection**: Check Actuator endpoints are exposed at `/actuator/prometheus`

### Health Checks
```bash
# Application health
curl http://localhost:8080/api/actuator/health

# Database connectivity
curl http://localhost:8080/api/actuator/health/db

# Redis connectivity
curl http://localhost:8080/api/actuator/health/redis

# Elasticsearch health
curl -u elastic:elastic http://localhost:9200/_cluster/health

# Prometheus targets
curl http://localhost:9090/api/v1/targets
```

## Security Implementation

### Authentication System
The application implements a custom Spring Security authentication system:

- **CustomUserDetails**: Extends UserDetails with additional `memberId` field for business logic
- **CustomUserDetailsService**: Loads users by email, validates member status (ACTIVE only)
- **Member Authentication**: Uses email as username, validates member status before authentication
- **Role-based Security**: Supports USER and ADMIN roles via `Role` enum
- **Status Management**: Members can be ACTIVE or SUSPENDED via `Status` enum

### Member Domain Design
The member module follows Domain-Driven Design with value objects:

- **Member Entity**: Core aggregate root with embedded Email and Password value objects
- **Email Value Object**: Encapsulates email validation and business rules
- **Password Value Object**: Handles password encoding and security concerns
- **Role Enum**: Defines user permissions (USER, ADMIN with authority keys)
- **Status Enum**: Manages member lifecycle (ACTIVE, SUSPENDED)
- **Repository Pattern**: `MemberJpaRepository` with custom email-based queries

### Development Best Practices
- Use Spring Modulith events for module communication
- Follow the established package structure for new features
- Add proper logging with structured fields for observability
- Include actuator health indicators for custom components
- Write integration tests for module boundaries
- Use OpenAPI annotations for API documentation
- Follow the existing exception handling patterns
- Use value objects (Email, Password) for domain modeling
- Validate member status in authentication flow
- Apply role-based access control with custom authorities

## Development Rules

### Test Implementation Rules
- **Method Names**: Use `andExpect()` instead of `andExpected()` in MockMvc tests
- **Required Imports**: Always import `RestDocsConfiguration` and `TestOAuth2Config` for integration tests
- **Annotations**: Use `@MockitoBean` instead of deprecated `@MockBean`
- **Test Data**: Create real test entities instead of using hardcoded IDs
- **WebMvcTest Setup**: For unit tests, use `@AutoConfigureMockMvc(addFilters = false)` to disable security filters
- **Integration Test Auth**: Use `@WithMockUser` with `.with(user(customUserDetails))` for authenticated requests
- **CSRF Protection**: Add `.with(csrf())` to POST/PUT/DELETE test requests when security is enabled

### API Implementation Rules
- **Logging**: Do NOT add manual log statements in controllers - AOP handles logging automatically
- **Controller Pattern**: Follow existing controller patterns (refer to `AIModelDetailController`):
  - Use `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`
  - Add `@Tag` annotation for Swagger documentation
  - Use `@Operation` and `@ApiResponses` for method documentation
  - Extract `memberId` from `@AuthenticationPrincipal CustomUserDetails`
  - Return responses using `ResponseEntity.ok(ApiUtils.success(data))` format
- **Authentication**: Always use `CustomUserDetails.getMemberId()` for member identification
- **Response Format**: Use `ApiUtils` for standardized response wrapping

### Test Data Management
- **Comprehensive Test Dataset**: Use `/sql/comprehensive-test-data.sql` for complete project test data
- **@Sql Annotation Usage**: Load test data with `@Sql("/sql/comprehensive-test-data.sql")`
- **Selective Data Loading**: Use specific SQL files like `/sql/basic-members.sql` for lighter tests
- **Available Test Data**:
  - **Members**: 8 users with different roles (USER, ADMIN) and statuses (ACTIVE, SUSPENDED)
  - **AI Models**: 6 models including public, private, and system models
  - **Subscriptions**: 4 subscription plans with active/expired member subscriptions
  - **Points**: Point policies, balances, and transaction history
  - **Coupons**: Active and expired coupons with various discount types
  - **Reviews**: Model reviews with different ratings and statuses
  - **Reports**: Sample reports for different target types and statuses
  - **Files**: Model preview images and profile pictures
- **Pre-configured Test Users**:
  - ID 1: `normalUser` (normal@test.com) - Basic user
  - ID 2: `premiumUser` (premium@test.com) - User with premium subscription  
  - ID 3: `adminUser` (admin@test.com) - Administrator
  - ID 4: `suspendedUser` (suspended@test.com) - Suspended user
  - ID 5: `activeCreator` (creator@test.com) - Active model creator with multiple models
- **Test Data Examples**:
  ```java
  @Test
  @Sql("/sql/comprehensive-test-data.sql")
  void testWithPreloadedData() {
      // All test data is available immediately
      var member = memberRepository.findById(1L).orElseThrow();
      var models = aiModelRepository.findByOwnerId(5L); // activeCreator's models
  }
  ```