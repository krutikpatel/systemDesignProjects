# Algorithms

## Token Bucket

Analogy: a bucket fills with tokens over time; each request spends one token.
Use when you want burst tolerance with controlled average rate.

## Sliding Window

Analogy: a moving ruler over recent timestamps; only exact recent requests count.
Use when you need strict rolling-window fairness without boundary bursts.

## Leaky Bucket

Analogy: a queue leaking at a fixed pace; throughput is smoothed.
Use when downstream services need steady request flow.

## Decision Table

| Use Case | Recommended Algorithm | Why |
|---|---|---|
| Public API with bursty traffic | Token Bucket | Allows short bursts while capping sustained rate |
| Payment processing pipeline | Leaky Bucket | Smooth output protects strict downstream SLAs |
| Per-minute strict quota | Sliding Window | Accurate rolling-window enforcement |

## Example `policies.yaml`

```yaml
policies:
  - id: "public-api"
    algorithm: TOKEN_BUCKET
    limit: 200
    refillRate: 20
    capacity: 200

  - id: "payments"
    algorithm: LEAKY_BUCKET
    limit: 50
    leakRate: 10
    capacity: 50

  - id: "strict-minute"
    algorithm: SLIDING_WINDOW
    limit: 60
    windowMs: 60000
```

## Performance and Memory Trade-offs

- Token Bucket: low memory per key, fast, burst-friendly.
- Sliding Window: highest memory due to timestamp tracking, best precision.
- Leaky Bucket: low-medium memory, best smoothing for fixed-rate outputs.
