# Logging Configuration Guide

This document describes the profile-specific Logstash appender configurations optimized for different environments.

## Overview

The application now uses separate Logstash appender configurations for different Spring profiles:

- **Local Profile**: Maximum debugging capabilities with immediate log visibility
- **Dev Profile**: Balanced debugging features with reasonable performance
- **Production Profile**: High-throughput optimized configuration with async processing

## Profile Configurations

### Local Profile (`local`)
**File**: `logstash-local-appender.xml`
**Purpose**: Maximum debugging capabilities for local development

**Characteristics**:
- Immediate log visibility (ring buffer: 32, immediate flush: true)
- Full caller data including method names and line numbers
- Pretty-printed JSON for manual inspection
- Complete logger names (no truncation)
- All MDC, structured arguments, and context included
- Very fast reconnection (2 seconds)

**Best For**:
- Active development and debugging
- Investigating complex issues
- Learning application behavior

### Dev Profile (`dev`)
**File**: `logstash-dev-appender.xml`
**Purpose**: Real-time debugging with acceptable performance impact

**Characteristics**:
- Small ring buffer (64) for quick log visibility
- Caller data included for debugging
- Detailed logger names (50 characters)
- Pretty-printed JSON for readability
- All debugging context preserved
- Moderate reconnection delay (5 seconds)

**Best For**:
- Development environment
- Integration testing
- Pre-production debugging

### Production Profile (`prod`)
**File**: `logstash-prod-appender.xml` or `logstash-prod-async-appender.xml`
**Purpose**: Maximum throughput with minimal performance impact

**Standard Production Appender**:
- Large ring buffer (512) for efficient batching
- No caller data for performance
- Shortened logger names (25 characters)
- Compact JSON format
- Batched flushing for throughput
- Extended timeouts for stability

**Async Production Appender** (Optional):
- Async wrapper with 2048 queue size
- Never blocks application threads
- Even larger ring buffer (1024)
- Automatic discarding under pressure
- Maximum throughput optimization

## Configuration Details

### Performance Impact Comparison

| Feature | Local | Dev | Prod | Prod-Async |
|---------|-------|-----|------|------------|
| Caller Data | ✅ | ✅ | ❌ | ❌ |
| Pretty Print | ✅ | ✅ | ❌ | ❌ |
| Immediate Flush | ✅ | ✅ | ❌ | ❌ |
| Ring Buffer Size | 32 | 64 | 512 | 1024 |
| Queue Size | N/A | N/A | N/A | 2048 |
| Thread Blocking | Yes | Yes | Yes | Never |

### Log Level Settings by Profile

**Local Profile**:
- Root level: DEBUG
- Application (nomodel): DEBUG
- Spring Web: DEBUG  
- Spring Security: DEBUG

**Dev Profile**:
- Root level: INFO
- Application (nomodel): DEBUG
- Spring Framework: INFO

**Production Profile**:
- Root level: INFO
- Application (nomodel): INFO
- Spring Framework: WARN
- Hibernate: WARN
- HikariCP: WARN

### Field Mappings

All configurations use consistent field mappings for Kibana compatibility:

```json
{
  "@timestamp": "Event timestamp",
  "message": "Log message content",
  "log_level": "Log level (DEBUG, INFO, WARN, ERROR)",
  "thread_name": "Thread that generated the log",
  "logger_name": "Logger class/category",
  "application": "nomodel",
  "environment": "local/development/production",
  "service": "spring-boot",
  "version": "1.0.0",
  "profile": "Profile name for identification"
}
```

## Usage Guidelines

### Choosing the Right Configuration

1. **Use Local Profile When**:
   - Actively developing new features
   - Debugging complex issues
   - Learning application flow
   - Performance impact is not a concern

2. **Use Dev Profile When**:
   - Running integration tests
   - Performing code reviews
   - Pre-production validation
   - Need debugging info but care about performance

3. **Use Production Profile When**:
   - Running in production environments
   - Performance is critical
   - Log volume is high
   - Stability is paramount

### Switching Between Configurations

To use a specific profile, set the Spring active profile:

```bash
# Local development with maximum debugging
./gradlew bootRun --args='--spring.profiles.active=local'

# Development environment
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production environment
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### ELK Stack Integration

All configurations work with the existing ELK stack setup:

1. Start ELK stack: `docker compose -f docker-compose-elk.yml up -d`
2. Run application with desired profile
3. View logs in Kibana: http://localhost:5601
4. Use index pattern: `nomodel-logs-*`

### Troubleshooting

**Logs not appearing in Kibana**:
1. Check Logstash connectivity: `telnet localhost 5001`
2. Verify ELK stack is running: `docker compose -f docker-compose-elk.yml ps`
3. Check application logs for connection errors

**Poor logging performance**:
1. Switch to production profile for high-throughput scenarios
2. Consider using async appender for maximum throughput
3. Adjust ring buffer sizes based on log volume

**Missing debug information**:
1. Ensure correct profile is active
2. Check log level configurations
3. Verify caller data settings match requirements

## Advanced Configuration

### Custom Environment Variables

You can customize Logstash destination via environment variables:

```yaml
# application.yml
logging:
  logstash:
    host: ${LOGSTASH_HOST:localhost}
    port: ${LOGSTASH_PORT:5001}
```

### Performance Tuning

For high-volume environments, consider:

1. **Increase Ring Buffer Size**: Adjust `ringBufferSize` based on log volume
2. **Use Async Appenders**: Enable async processing for maximum throughput
3. **Adjust Queue Sizes**: Tune async queue sizes based on memory availability
4. **Monitor Discarded Events**: Track events lost due to queue overflow

### Security Considerations

- Ensure Logstash endpoint is secured in production
- Consider TLS encryption for log transport
- Sanitize sensitive data before logging
- Implement log retention policies