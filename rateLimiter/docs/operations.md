# Operations Runbook

## Circuit Breaker State Machine

```text
CLOSED --(failureThreshold reached)--> OPEN
OPEN --(cooldown elapsed)--> HALF_OPEN
HALF_OPEN --(successThreshold reached)--> CLOSED
HALF_OPEN --(any failure)--> OPEN
```

## Manual Circuit Reset

Use administrative access to call `CircuitBreakerStoreAdapter.reset()` on the running instance (for example through internal admin hooks or controlled maintenance command path).

## Fail-Open vs Fail-Closed

- `OPEN`: prefer availability; requests continue when Redis is down.
- `CLOSED`: prefer strict protection; requests fail when Redis is down.

Example choices:
- Public read APIs: often `OPEN`.
- Billing-critical endpoints: often `CLOSED`.

## Incident: Redis Outage

Symptoms:
- Rising store errors, degraded health checks.

Diagnosis:
- Check Redis reachability and service logs for store exceptions.

Resolution:
- Restore Redis connectivity, confirm health check returns `storeOk=true`, monitor breaker closing.

## Incident: Throttle Rate Spike

Symptoms:
- Sudden jump in throttled responses.

Diagnosis:
- Inspect `drl_requests_total` allowed vs throttled ratio by policy.

Resolution:
- Validate traffic pattern and policy values, then reload tuned config.

## Incident: Misconfigured Policy Deployed

Symptoms:
- Immediate `INVALID_ARGUMENT` on reload or abnormal throttle behavior.

Diagnosis:
- Review reload payload and validation errors.

Resolution:
- Roll back to known-good policy set and re-run reload.

## Incident: Service OOM

Symptoms:
- Container restarts, memory pressure alerts.

Diagnosis:
- Check heap usage, active key cardinality, and traffic profile.

Resolution:
- Reduce high-cardinality keys, tune memory, and scale horizontally.

## Example Prometheus Alerts

```yaml
groups:
  - name: drl-alerts
    rules:
      - alert: DRLHighThrottleRate
        expr: sum(rate(drl_requests_total{result="throttled"}[5m])) / sum(rate(drl_requests_total[5m])) > 0.20
        for: 5m
        labels:
          severity: warning
      - alert: DRLStoreErrorRate
        expr: sum(rate(drl_store_errors_total[5m])) > 0.01
        for: 5m
        labels:
          severity: critical
      - alert: DRLHighP99Latency
        expr: histogram_quantile(0.99, sum(rate(drl_store_latency_seconds_bucket[5m])) by (le)) > 0.01
        for: 10m
        labels:
          severity: warning
```
