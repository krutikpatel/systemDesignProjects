# Code Review Fix Progress

## Baseline
- Date: 2026-03-06
- Source plan: `CODE_REVIEW_REMEDIATION_REPORT.md`
- Initial status: implementation started
- Baseline verification: `./gradlew test` passed before changes

## Fix Log

| Step | Fix | Status | Unit Test After Fix | Notes |
|---|---|---|---|---|
| 0 | Baseline setup + tracker creation | Done | `./gradlew test` passed (pre-change) | Ready for sequential remediation |
| 1 | Runtime wiring in `Main` (metrics server/registry, circuit breaker wrapping, deadline+logging interceptors, validated env parsing) | Done | `./gradlew test` passed | Added `Main.RuntimeConfig` and wiring/config unit tests |
| 2 | Strict proto algorithm mapping (no silent fallback) with validation errors on reload path | Done | `./gradlew test` passed | Added tests for `ALGORITHM_UNSPECIFIED` and `UNRECOGNIZED` |
| 3 | Removed implicit `RateLimitPolicy` defaults and enforced TOKEN_BUCKET `limit == capacity` invariant in validation | Done | `./gradlew test` passed | Added policy-loader invariant test and updated concurrent reload test data |
| 4 | Converted `AlgorithmFactory` to singleton strategy instances (no per-request executor allocation) | Done | `./gradlew test` passed | Factory now uses immutable enum map with shared executors |
| 5 | Simplified `RedisStoreAdapter` execution path and made `set` TTL millisecond-accurate via `PSETEX` | Done | `./gradlew test` passed | Removed per-call `StructuredTaskScope` orchestration |
| 6 | Hardened gRPC error handling with runtime fallback to `INTERNAL` and failure logging | Done | `./gradlew test` passed | Added `RateLimiterGrpcHandlerTest` coverage for unexpected check/reload failures |
| 7 | Made in-memory store algorithm state mutations thread-safe and added concurrency coverage | Done | `./gradlew test` passed | Sliding window and token bucket state updates now synchronized per key/state |
| 8 | Health-check now reflects circuit breaker state (degraded when breaker is not closed) | Done | `./gradlew test` passed | Added health test covering OPEN circuit behavior |
| 9 | Cleanup: removed unused exception and fixed one-time gauge registration pattern | Done | `./gradlew test` passed | `UnknownAlgorithmException` removed; bucket fill gauge now registered once per policy |
| 10 | Documentation alignment for runtime/error/validation behavior (`design`, `configuration`, `api`) | Done | `./gradlew test` passed | Added strict enum + health semantics + token bucket invariant notes |
