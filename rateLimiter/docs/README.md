# Distributed Rate Limiter

Distributed Rate Limiter (DRL) is a standalone Java 21 gRPC service that enforces per-key traffic limits across multiple instances using Redis for shared atomic state. It supports Token Bucket, Sliding Window, and Leaky Bucket policies. It is designed for API gateways and microservices that need consistent throttle decisions, runtime policy reloads, and observability.

## Requirements

- Java 21
- Redis (required for running the service outside tests)
- Docker + Docker Compose (optional for local runtime; required for containerized runs)
- `grpcurl` (optional, for manual API checks)

Notes:
- Yes, this project needs Redis running for normal service execution.
- Unit tests (`./gradlew test`) do not require a local Redis instance.
- Integration tests use Testcontainers and require Docker running.

## Run the Project

### Option 1: Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

This starts both Redis and the rate limiter service.

### Option 2: Run locally (non-Docker)

1. Start Redis (example):
```bash
docker run --rm -p 6379:6379 redis:7-alpine
```

2. Run the service:
```bash
REDIS_URI=redis://localhost:6379 GRPC_PORT=50051 FAIL_MODE=OPEN ./gradlew run
```

## Test the Running Service

`CheckRateLimit`:
```bash
grpcurl -plaintext -d '{"policyId":"default","key":"user-1","path":"/v1/payments","method":"GET"}' \
  localhost:50051 ratelimiter.v1.RateLimiterService/CheckRateLimit
```

`ReloadConfig`:
```bash
grpcurl -plaintext -d '{"policies":[{"policyId":"default","algorithm":"TOKEN_BUCKET","limit":100,"refillRate":10,"capacity":100}]}' \
  localhost:50051 ratelimiter.v1.RateLimiterService/ReloadConfig
```

`HealthCheck`:
```bash
grpcurl -plaintext -d '{}' \
  localhost:50051 ratelimiter.v1.RateLimiterService/HealthCheck
```

## Run Tests

Unit tests:
```bash
./gradlew test
```

Integration tests (separate suite):
```bash
./gradlew integrationTest
```

All tests:
```bash
./gradlew test integrationTest
```

## Documentation

- [Configuration](./configuration.md)
- [gRPC API](./api.md)
- [Algorithms](./algorithms.md)
- [Operations Runbook](./operations.md)
