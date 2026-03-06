# Distributed Rate Limiter — Story Progress Tracker

This file tracks completion progress for `drl_agent_stories_java21_grpc.md`.
A story should be logged here only when its implementation is complete and `./gradlew test` passes.

## Rules
- Add one entry per completed story.
- Keep entries in story order (`STORY-001` to `STORY-017`).
- Include completion date and any notable details (bugs, tradeoffs, follow-ups).

## Progress Summary
- Total stories: 17
- Completed stories: 17
- Remaining stories: 0
- Last updated: 2026-03-05

## Completed Stories Log

| Story ID | Title | Completed On | Tests Status | Notable Details |
|---|---|---|---|---|
| STORY-001 | Initialize Gradle Project & Module Structure | 2026-03-05 | `./gradlew test` passed | Bootstrapped Java 21 Gradle project, CI workflow, package layout, `Main.java`, and baseline `MainTest`; updated Shadow plugin/task wiring for Gradle 9 compatibility. |
| STORY-002 | Define Protobuf Service Contract | 2026-03-05 | `./gradlew test` passed | Added `rate_limiter.proto`; verified `./gradlew generateProto` and generated classes including `RateLimiterServiceGrpc`. |
| STORY-003 | Define Internal Domain Model | 2026-03-05 | `./gradlew test` passed | Added model enums/records and `Errors` exception container with validation error accessor; added `ModelTest` for equality/hash and copy-with-modification behavior. |
| STORY-004 | Implement Redis Store Abstraction | 2026-03-05 | `./gradlew test` passed | Added `StoreAdapter`, `RedisStoreAdapter` with `StructuredTaskScope` wrappers, and `InMemoryStoreAdapter` script-dispatch simulation (`token_bucket`, `sliding_window`, `leaky_bucket`) plus adapter unit tests. |
| STORY-005 | Implement Config Loader | 2026-03-05 | `./gradlew test` passed | Added classpath YAML policies, `PolicyLoader` with full validation error aggregation, atomic reload map swap, and concurrency/validation test coverage. |
| STORY-006 | Implement Token Bucket Algorithm | 2026-03-05 | `./gradlew test` passed | Added `AlgorithmExecutor` and `TokenBucketExecutor` Lua-driven evaluation flow with deterministic clock injection for tests; covered initial allow, burst/reject, decrement, refill, and retry-after behavior. |
| STORY-007 | Implement Sliding Window Algorithm | 2026-03-05 | `./gradlew test` passed | Added `SlidingWindowExecutor` with sorted-set Lua flow and deterministic-time unit tests for limit enforcement, positive retry-after, and window-slide slot reopening. |
| STORY-008 | Implement Leaky Bucket, Algorithm Factory & Core Service | 2026-03-05 | `./gradlew test` passed | Added `LeakyBucketExecutor` delegation flow, exhaustive `AlgorithmFactory`, and core `RateLimiterService` with fail-open/fail-closed behavior and throttle callback tests. |
| STORY-009 | Implement gRPC Service Handler | 2026-03-05 | `./gradlew test` passed | Added `ProtoMapper` and `RateLimiterGrpcHandler` with status mapping for `NOT_FOUND`, `UNAVAILABLE`, and `INVALID_ARGUMENT`, plus mapper/handler unit tests. |
| STORY-010 | Bootstrap gRPC Server & Wiring | 2026-03-05 | `./gradlew test` passed | Updated `Main` composition root with env-based config, virtual-thread gRPC executor, graceful shutdown hook, and added `LoggingInterceptor`; added in-process smoke tests including virtual-thread assertion. |
| STORY-011 | Implement Prometheus Metrics | 2026-03-05 | `./gradlew test` passed | Added `MetricsRegistry` and `MetricsHttpServer`, integrated optional request/store metrics hooks into service and Redis adapter, and added tests for counters, `/metrics`, `/healthz`, and virtual-thread executor. |
| STORY-012 | Implement Structured Logging | 2026-03-05 | `./gradlew test` passed | Added JSON Logback config plus test-time log suppression; updated gRPC handler to emit structured marker-based decision logs with `latencyMs` and throttling fields. |
| STORY-013 | Implement Circuit Breaker | 2026-03-05 | `./gradlew test` passed | Added atomic-state `CircuitBreakerStoreAdapter` with injected clock, threshold/cooldown transitions (`CLOSED`/`OPEN`/`HALF_OPEN`), and transition/short-circuit unit tests. |
| STORY-014 | Implement Deadline Interceptor | 2026-03-05 | `./gradlew test` passed | Added gRPC `DeadlineInterceptor` enforcing expired/low-budget deadline rejection with `DEADLINE_EXCEEDED`, warning logs, and in-process deadline behavior tests. |
| STORY-015 | Dockerize the Service | 2026-03-05 | `./gradlew test` passed | Added multi-stage Docker build plus `docker-compose.yml` and `docker-compose.test.yml` for local runtime and test Redis wiring. |
| STORY-016 | Write Integration Tests with Testcontainers | 2026-03-05 | `./gradlew test` passed | Added integration test base/classes and Gradle `integrationTest` task/source set; unit test task now excludes integration package and CI adds separate `integration` job after `test`. |
| STORY-017 | Write Public Documentation | 2026-03-05 | `./gradlew test` passed | Added complete `docs/` set (`README`, `configuration`, `api`, `algorithms`, `operations`) with runnable command examples and runbook content. |

## Entry Template (Copy for next completion)

| Story ID | Title | Completed On | Tests Status | Notable Details |
|---|---|---|---|---|
| STORY-XXX | <story title> | YYYY-MM-DD | `./gradlew test` passed | <key implementation notes, risks, or follow-ups> |
