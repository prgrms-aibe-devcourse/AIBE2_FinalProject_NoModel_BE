# Logstash Appender Configuration Reference

This directory contains profile-specific Logstash appender configurations optimized for different environments.

## Quick Reference

| Profile | Configuration File | Optimization | Ring Buffer | Flush Mode | Caller Data |
|---------|-------------------|--------------|-------------|------------|-------------|
| `local` | `logstash-local-appender.xml` | Max debugging | 32 | Immediate | ✅ Full |
| `dev` | `logstash-dev-appender.xml` | Real-time debugging | 64 | Immediate | ✅ Full |  
| `prod` | `logstash-prod-appender.xml` | High throughput | 512 | Batched | ❌ None |
| `prod` | `logstash-prod-async-appender.xml` | Max throughput | 1024 + 2048 queue | Async | ❌ None |

## Configuration Features

### Development Optimizations (local/dev)
- **Immediate Log Visibility**: Small ring buffers with immediate flushing
- **Rich Debug Context**: Caller data, pretty-printed JSON, full logger names
- **Fast Reconnection**: Quick recovery from connection failures
- **Maximum Information**: All MDC, structured arguments, and context included

### Production Optimizations (prod)
- **High Throughput**: Large ring buffers with batched flushing  
- **Minimal Overhead**: No caller data, compact JSON, shortened logger names
- **Stability Focus**: Extended timeouts and connection keep-alive
- **Optional Async**: Non-blocking async wrapper with large queue

## Usage Examples

```bash
# Maximum debugging capabilities
--spring.profiles.active=local

# Balanced debugging for development
--spring.profiles.active=dev

# High-performance production logging
--spring.profiles.active=prod
```

## Performance Impact

- **Local**: Highest overhead, maximum information
- **Dev**: Moderate overhead, good debugging balance
- **Prod**: Minimal overhead, production-ready throughput
- **Prod-Async**: Near-zero overhead, never blocks application threads

See `/docs/LOGGING_CONFIGURATION.md` for detailed configuration guide.