# Configuration

## Environment Variables

| Name | Type | Default | Description |
|---|---|---|---|
| `REDIS_URI` | string | `redis://localhost:6379` | Redis endpoint used by store adapter |
| `GRPC_PORT` | integer | `50051` | gRPC server listen port |
| `METRICS_PORT` | integer | `9090` | Metrics HTTP server listen port |
| `FAIL_MODE` | enum (`OPEN`/`CLOSED`) | `OPEN` | Behavior when store is unavailable |

## `policies.yaml` Reference

| Field | Type | Applies To | Valid Range / Rules |
|---|---|---|---|
| `id` | string | all | non-empty, unique |
| `algorithm` | enum | all | `TOKEN_BUCKET`, `SLIDING_WINDOW`, `LEAKY_BUCKET` |
| `limit` | integer | all | `> 0` |
| `windowMs` | integer (ms) | `SLIDING_WINDOW` | `> 0` |
| `refillRate` | integer (tokens/sec) | `TOKEN_BUCKET` | `> 0` |
| `capacity` | integer | `TOKEN_BUCKET`, `LEAKY_BUCKET` | `> 0` |
| `leakRate` | integer (req/sec) | `LEAKY_BUCKET` | `> 0` |

Additional validation:
- `TOKEN_BUCKET` policies must satisfy `limit == capacity`.
- `ReloadConfig` rejects `ALGORITHM_UNSPECIFIED` and unrecognized enum values with `INVALID_ARGUMENT`.

## Live Reload via gRPC

Use `ReloadConfig` to update policies without service restart:

```bash
grpcurl -plaintext -d '{
  "policies": [
    {
      "policyId": "default",
      "algorithm": "TOKEN_BUCKET",
      "limit": 200,
      "refillRate": 20,
      "capacity": 200
    }
  ]
}' localhost:50051 ratelimiter.v1.RateLimiterService/ReloadConfig
```
