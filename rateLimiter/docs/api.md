# gRPC API

Service: `ratelimiter.v1.RateLimiterService`

## `CheckRateLimit`

Request fields:
- `policyId` (string)
- `key` (string)
- `path` (string)
- `method` (string)

Response fields:
- `allowed` (bool): request accepted/rejected
- `limit` (int): configured max
- `remaining` (int): remaining quota
- `resetAt` (int64): unix timestamp (seconds)
- `retryAfter` (int): seconds to wait when denied

Example:
```bash
grpcurl -plaintext -d '{"policyId":"default","key":"u1","path":"/v1/orders","method":"GET"}' \
  localhost:50051 ratelimiter.v1.RateLimiterService/CheckRateLimit
```

## `ReloadConfig`

Request:
- `policies[]` (`PolicyConfig`)

Response:
- `success` (bool)
- `policyCount` (int)
- `message` (string)

Example:
```bash
grpcurl -plaintext -d '{"policies":[{"policyId":"strict","algorithm":"SLIDING_WINDOW","limit":20,"windowMs":60000}]}' \
  localhost:50051 ratelimiter.v1.RateLimiterService/ReloadConfig
```

## `HealthCheck`

Request: empty object

Response:
- `status` (`ok`/`degraded`)
- `storeOk` (bool)

Example:
```bash
grpcurl -plaintext -d '{}' \
  localhost:50051 ratelimiter.v1.RateLimiterService/HealthCheck
```

## Status Codes

- `NOT_FOUND`: policy ID does not exist
- `INVALID_ARGUMENT`: reload config validation failed
- `UNAVAILABLE`: store unavailable and fail mode is closed
- `DEADLINE_EXCEEDED`: request deadline already expired or under minimum budget
