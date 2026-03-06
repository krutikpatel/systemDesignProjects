# Distributed Rate Limiter — Agent Coding Stories
## Standalone Service · Java 21 · gRPC · Docker

> **How to use this file:** Each story is a self-contained unit of work for an AI coding agent.
> Stories must be completed **in order within each phase** — later stories depend on earlier ones.
> Each story includes context, exact method signatures, file paths, and acceptance criteria the agent can verify by running tests.

---

## Stack Decisions

| Concern | Choice | Rationale |
|---|---|---|
| Language | Java 21 (LTS) | Records, sealed classes, virtual threads — modern Java without extra runtime |
| Build tool | Gradle (Groovy DSL) | Industry standard for JVM projects |
| Async model | Virtual Threads (`Thread.ofVirtual()`) | Java 21 replaces coroutines/CompletableFuture for I/O-bound concurrency |
| gRPC framework | `grpc-java` + `protobuf` | Official Java gRPC bindings, battle-tested at scale |
| Redis client | Lettuce | Async Redis client; wrap with virtual threads for blocking-style code |
| Testing | JUnit 5 + Mockito + Testcontainers | Standard Java testing stack |
| Containerization | Docker + Docker Compose | Single-JAR deployment, no orchestrator required |
| Metrics | Micrometer + Prometheus | JVM-standard metrics with Prometheus scrape endpoint |
| Logging | Logback + SLF4J + logstash-logback-encoder | Structured JSON logs |

---

## Phase 1 — Foundation

---

### STORY-001 · Initialize Gradle Project & Module Structure

**Goal:** Bootstrap a Java 21 Gradle project with the correct folder structure, dependency versions, and CI pipeline.

**Context:** All future stories build on this scaffold. The project is a standalone gRPC server — no Spring, no framework. Keep it lean: `grpc-java`, `lettuce`, and standard JVM libraries only.

**Instructions:**

1. Create a Gradle project using Groovy DSL (`build.gradle`)
2. Set Java toolchain to version 21 in `build.gradle`:
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```
3. Enable preview features for Java 21 (needed for virtual threads stability):
```groovy
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ["--enable-preview"]
}
tasks.withType(Test).configureEach {
    jvmArgs "--enable-preview"
}
tasks.withType(JavaExec).configureEach {
    jvmArgs "--enable-preview"
}
```
4. Add these dependencies to `build.gradle`:
```groovy
// gRPC + Protobuf
implementation "io.grpc:grpc-stub:1.60.0"
implementation "io.grpc:grpc-protobuf:1.60.0"
implementation "io.grpc:grpc-netty-shaded:1.60.0"
implementation "com.google.protobuf:protobuf-java:3.25.1"
compileOnly     "org.apache.tomcat:annotations-api:6.0.53"

// Redis
implementation "io.lettuce:lettuce-core:6.3.0.RELEASE"

// Config
implementation "org.yaml:snakeyaml:2.2"

// Metrics
implementation "io.micrometer:micrometer-registry-prometheus:1.12.0"

// Logging
implementation "ch.qos.logback:logback-classic:1.4.11"
implementation "net.logstash.logback:logstash-logback-encoder:7.4"
implementation "org.slf4j:slf4j-api:2.0.9"

// Testing
testImplementation "org.junit.jupiter:junit-jupiter:5.10.1"
testImplementation "org.mockito:mockito-core:5.8.0"
testImplementation "org.mockito:mockito-junit-jupiter:5.8.0"
testImplementation "org.testcontainers:testcontainers:1.19.3"
testImplementation "org.testcontainers:junit-jupiter:1.19.3"
```
5. Configure the `protobuf` Gradle plugin to generate Java code from `.proto` files in `src/main/proto/`
6. Configure `shadowJar` plugin for fat JAR generation
7. Create the following package structure under `src/main/java/com/ratelimiter/`:
```
algorithms/
config/
grpc/
metrics/
store/
model/
Main.java
```
8. `Main.java` should have a `main()` method that prints `"Rate Limiter Service starting..."`
9. Add `.github/workflows/ci.yml` that runs `./gradlew test` on every push to `main`

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- `./gradlew build` compiles without errors
- `./gradlew test` runs successfully (zero tests is acceptable at this stage)
- Proto plugin is configured and ready (even without `.proto` files yet)
- CI workflow YAML is valid and present

**Files to create:** `build.gradle`, `settings.gradle`, `gradle.properties`, `src/main/java/com/ratelimiter/Main.java`, `.github/workflows/ci.yml`

---

### STORY-002 · Define Protobuf Service Contract

**Goal:** Define the gRPC service contract as a `.proto` file that all service components implement and all callers use to generate client stubs.

**Context:** The proto file is the public API surface of this service. It must be defined before any implementation. All API gateways and microservices calling this rate limiter will generate client stubs from this file.

**Instructions:**

Create `src/main/proto/ratelimiter/v1/rate_limiter.proto`:

```protobuf
syntax = "proto3";

package ratelimiter.v1;

option java_package         = "com.ratelimiter.grpc.proto";
option java_multiple_files  = true;
option java_outer_classname = "RateLimiterProto";

// Core rate limit check
message CheckRateLimitRequest {
  string policy_id = 1;  // Which policy to evaluate
  string key       = 2;  // Limit key: user ID, IP, API key, etc.
  string path      = 3;  // Request path (for logging/metrics)
  string method    = 4;  // HTTP method (for logging/metrics)
}

message CheckRateLimitResponse {
  bool   allowed     = 1;  // Whether the request is permitted
  int32  limit       = 2;  // Max requests allowed
  int32  remaining   = 3;  // Requests remaining
  int64  reset_at    = 4;  // Unix timestamp (seconds) when limit resets
  int32  retry_after = 5;  // Seconds to wait (only when allowed=false)
}

// Admin: reload policies at runtime without restart
message ReloadConfigRequest {
  repeated PolicyConfig policies = 1;
}

message ReloadConfigResponse {
  bool   success      = 1;
  int32  policy_count = 2;
  string message      = 3;
}

message PolicyConfig {
  string    policy_id   = 1;
  Algorithm algorithm   = 2;
  int32     limit       = 3;
  int64     window_ms   = 4;  // sliding_window only
  int32     refill_rate = 5;  // token_bucket: tokens/sec
  int32     capacity    = 6;  // token_bucket and leaky_bucket
  int32     leak_rate   = 7;  // leaky_bucket: requests/sec
}

enum Algorithm {
  ALGORITHM_UNSPECIFIED = 0;
  TOKEN_BUCKET          = 1;
  SLIDING_WINDOW        = 2;
  LEAKY_BUCKET          = 3;
}

// Health check
message HealthCheckRequest  {}
message HealthCheckResponse {
  string status   = 1;  // "ok" or "degraded"
  bool   store_ok = 2;
}

service RateLimiterService {
  rpc CheckRateLimit (CheckRateLimitRequest) returns (CheckRateLimitResponse);
  rpc ReloadConfig   (ReloadConfigRequest)   returns (ReloadConfigResponse);
  rpc HealthCheck    (HealthCheckRequest)    returns (HealthCheckResponse);
}
```

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- `./gradlew generateProto` succeeds and produces Java classes in `build/generated/`
- Generated classes include: `CheckRateLimitRequest`, `CheckRateLimitResponse`, `PolicyConfig`, `Algorithm` enum, `RateLimiterServiceGrpc`
- Proto file compiles with zero warnings

**Files to create:** `src/main/proto/ratelimiter/v1/rate_limiter.proto`

---

### STORY-003 · Define Internal Domain Model

**Goal:** Create Java 21 records and sealed classes for the internal domain model, fully decoupled from protobuf generated types.

**Context:** Never pass protobuf-generated objects into business logic. Define clean internal models and map from/to proto only at the gRPC boundary layer (STORY-009). This keeps algorithms and store logic free of protobuf dependencies and easy to unit test.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/model/Algorithm.java`:
```java
package com.ratelimiter.model;
public enum Algorithm { TOKEN_BUCKET, SLIDING_WINDOW, LEAKY_BUCKET }
```

2. Create `src/main/java/com/ratelimiter/model/FailMode.java`:
```java
package com.ratelimiter.model;
public enum FailMode { OPEN, CLOSED }
```

3. Create `src/main/java/com/ratelimiter/model/RateLimitPolicy.java` as a Java record:
```java
package com.ratelimiter.model;

public record RateLimitPolicy(
    String    id,
    Algorithm algorithm,
    int       limit,
    long      windowMs,     // sliding_window; default 60_000
    int       refillRate,   // token_bucket tokens/sec; default 10
    int       capacity,     // token_bucket and leaky_bucket; default 100
    int       leakRate      // leaky_bucket requests/sec; default 10
) {
    // Compact constructor for defaults
    public RateLimitPolicy {
        if (windowMs  <= 0) windowMs  = 60_000L;
        if (refillRate <= 0) refillRate = 10;
        if (capacity   <= 0) capacity   = 100;
        if (leakRate   <= 0) leakRate   = 10;
    }
}
```

4. Create `src/main/java/com/ratelimiter/model/RateLimitRequest.java` as a record:
```java
public record RateLimitRequest(
    String policyId, String key, String path, String method) {}
```

5. Create `src/main/java/com/ratelimiter/model/RateLimitResult.java` as a record:
```java
public record RateLimitResult(
    boolean allowed, int limit, int remaining,
    long resetAt, int retryAfter) {}
```

6. Create `src/main/java/com/ratelimiter/model/Errors.java` with all custom exceptions as nested static classes:
```java
package com.ratelimiter.model;
public final class Errors {
    public static class StoreException             extends RuntimeException { ... }
    public static class PolicyNotFoundException    extends RuntimeException { ... }
    public static class ConfigValidationException  extends RuntimeException {
        private final List<String> errors;
        ...
    }
    public static class UnknownAlgorithmException  extends RuntimeException { ... }
    public static class RateLimiterUnavailableException extends RuntimeException { ... }
}
```

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- All records compile with Java 21
- Record `equals`, `hashCode`, and `toString` work correctly (verify with a unit test)
- `ConfigValidationException` exposes `getErrors(): List<String>`
- Unit test in `src/test/java/com/ratelimiter/model/ModelTest.java` covers record equality and copy-with-modification using `withers` pattern (manual or via a builder)

**Files to create:** `src/main/java/com/ratelimiter/model/Algorithm.java`, `src/main/java/com/ratelimiter/model/FailMode.java`, `src/main/java/com/ratelimiter/model/RateLimitPolicy.java`, `src/main/java/com/ratelimiter/model/RateLimitRequest.java`, `src/main/java/com/ratelimiter/model/RateLimitResult.java`, `src/main/java/com/ratelimiter/model/Errors.java`, `src/test/java/com/ratelimiter/model/ModelTest.java`

---

### STORY-004 · Implement Redis Store Abstraction

**Goal:** Define a `StoreAdapter` interface and implement it with Lettuce, using Java 21 virtual threads to wrap async calls in clean blocking-style code.

**Context:** All three algorithm implementations call the store exclusively through this interface. The critical method is `eval()` — it executes a Lua script atomically on Redis, which is how all algorithms prevent race conditions across distributed nodes. Virtual threads make the async Lettuce API look synchronous without blocking OS threads.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/store/StoreAdapter.java`:
```java
public interface StoreAdapter {
    String  get(String key) throws StoreException;
    void    set(String key, String value, long ttlMs) throws StoreException;
    Object  eval(String script, List<String> keys, List<String> args) throws StoreException;
    boolean ping();
    void    close();
}
```

2. Create `src/main/java/com/ratelimiter/store/RedisStoreAdapter.java`:
   - Constructor: `RedisStoreAdapter(String redisUri)`
   - Use `io.lettuce.core.RedisClient` and `StatefulRedisConnection<String, String>`
   - Wrap all Lettuce async calls using virtual threads:
```java
// Pattern for all methods — run on a virtual thread so it looks synchronous
// but doesn't block a platform thread
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var task = scope.fork(() -> commands.get(key).get());
    scope.join().throwIfFailed();
    return task.get();
}
```
   - `eval(script, keys, args)` maps to: `commands.eval(script, ScriptOutputType.MULTI, keysArray, argsArray)`
   - Wrap all exceptions as `StoreException`
   - `ping()` calls Redis `PING`; returns `false` on any error — never throws

3. Create `src/main/java/com/ratelimiter/store/InMemoryStoreAdapter.java`:
   - Backed by `ConcurrentHashMap<String, Map.Entry<String, Long>>` (value + expiry ms)
   - `get()` returns `null` if key is expired or absent
   - `eval()` reads the first comment line `-- script: <name>` to dispatch to a Java method simulating that script's logic (implement for `token_bucket`, `sliding_window`, `leaky_bucket`)
   - `ping()` always returns `true`
   - Used in unit tests only

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- `RedisStoreAdapter` compiles and all interface methods are implemented
- Mockito-mocked Lettuce client passes all interface compliance tests
- `InMemoryStoreAdapter` passes identical interface tests without any mocking
- `ping()` never throws on connection failure — returns `false`
- Virtual thread usage verified: no `Thread.sleep()` blocking calls; all I/O through `StructuredTaskScope`
- Unit tests in `src/test/java/com/ratelimiter/store/`

**Files to create:** `src/main/java/com/ratelimiter/store/StoreAdapter.java`, `src/main/java/com/ratelimiter/store/RedisStoreAdapter.java`, `src/main/java/com/ratelimiter/store/InMemoryStoreAdapter.java`, `src/test/java/com/ratelimiter/store/RedisStoreAdapterTest.java`, `src/test/java/com/ratelimiter/store/InMemoryStoreAdapterTest.java`

---

### STORY-005 · Implement Config Loader

**Goal:** Load rate limit policies from a YAML file at startup, validate them, and support atomic hot-reload at runtime via the `ReloadConfig` gRPC endpoint.

**Context:** Policies are loaded from `config/policies.yaml` on the classpath. The same `PolicyLoader` is called by the gRPC handler when an operator sends a `ReloadConfig` request. Validation must catch every misconfiguration before it goes live.

**Instructions:**

1. Create `src/main/resources/config/policies.yaml`:
```yaml
policies:
  - id: "default"
    algorithm: TOKEN_BUCKET
    limit: 100
    refillRate: 10
    capacity: 100

  - id: "strict"
    algorithm: SLIDING_WINDOW
    limit: 20
    windowMs: 60000

  - id: "metered"
    algorithm: LEAKY_BUCKET
    limit: 50
    leakRate: 10
    capacity: 50
```

2. Create `src/main/java/com/ratelimiter/config/PolicyLoader.java`:
   - Parse YAML using `SnakeYAML` into a `List<Map<String, Object>>`, then map each entry to a `RateLimitPolicy` record
   - Validate on load:
     - `id` is non-empty and unique
     - `limit` > 0
     - `windowMs` > 0 when `algorithm == SLIDING_WINDOW`
     - `refillRate` > 0 and `capacity` > 0 when `algorithm == TOKEN_BUCKET`
     - `leakRate` > 0 and `capacity` > 0 when `algorithm == LEAKY_BUCKET`
   - Collect ALL validation errors before throwing — never fail-fast on the first error
   - Throw `ConfigValidationException(List<String> errors)` if any rule fails
   - Store active policies in `private volatile Map<String, RateLimitPolicy> policyMap`
   - `public RateLimitPolicy getPolicy(String id)` — throws `PolicyNotFoundException` if absent
   - `public void reload(List<RateLimitPolicy> newPolicies)` — validates then does an atomic reference swap using `synchronized` block around the map swap only (not around reads)

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- Valid YAML loads all 3 sample policies; each is accessible via `getPolicy()`
- Missing required field → `ConfigValidationException` listing every invalid field and policy ID
- Duplicate policy ID → `ConfigValidationException`
- `reload()` with invalid policies throws and leaves original policies intact
- Concurrent reads during `reload()` never observe a `null` or partial policy map
- Unit tests in `src/test/java/com/ratelimiter/config/PolicyLoaderTest.java`

**Files to create:** `src/main/resources/config/policies.yaml`, `src/main/java/com/ratelimiter/config/PolicyLoader.java`, `src/test/java/com/ratelimiter/config/PolicyLoaderTest.java`

---

## Phase 2 — Algorithms

---

### STORY-006 · Implement Token Bucket Algorithm

**Goal:** Implement the Token Bucket rate limiting algorithm using an atomic Redis Lua script.

**Context:** The check-and-update must be atomic to prevent over-admission when multiple service instances handle concurrent requests against the same key. The Lua script runs entirely on the Redis server — no round trips, no race conditions.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/algorithms/AlgorithmExecutor.java`:
```java
public interface AlgorithmExecutor {
    RateLimitResult check(String key, RateLimitPolicy policy, StoreAdapter store);
}
```

2. Create `src/main/java/com/ratelimiter/algorithms/TokenBucketExecutor.java`:
   - Redis key: `rl:tb:{policy.id}:{key}` stored as a Redis Hash with fields `tokens` and `lastRefill`
   - Define the Lua script as a `static final String` constant:
```lua
-- script: token_bucket
local key        = KEYS[1]
local capacity   = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now        = tonumber(ARGV[3])
local ttl        = tonumber(ARGV[4])

local data       = redis.call('HMGET', key, 'tokens', 'lastRefill')
local tokens     = tonumber(data[1]) or capacity
local lastRefill = tonumber(data[2]) or now

local elapsed    = math.max(0, now - lastRefill)
local newTokens  = math.min(capacity, tokens + elapsed * refillRate)

if newTokens >= 1 then
  redis.call('HMSET', key, 'tokens', newTokens - 1, 'lastRefill', now)
  redis.call('EXPIRE', key, ttl)
  return {1, math.floor(newTokens - 1), now + math.ceil(1 / refillRate)}
else
  return {0, 0, now + math.ceil(1 / refillRate)}
end
```
   - Call `store.eval(SCRIPT, List.of(redisKey), List.of(capacity, refillRate, nowSeconds, ttl))`
   - Parse the returned `List` into `RateLimitResult`
   - TTL: `(policy.capacity() / policy.refillRate()) * 2` seconds

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- First request always succeeds (bucket starts full)
- After `capacity` consecutive requests, the next is rejected with `allowed=false`
- `remaining` decrements correctly on each allowed request
- `retryAfter` is `ceil(1 / refillRate)` seconds on rejection
- All unit tests use `InMemoryStoreAdapter` — no real Redis
- Tests cover: initial allow, burst to capacity, rejection, refill after simulated time advance

**Files to create:** `src/main/java/com/ratelimiter/algorithms/AlgorithmExecutor.java`, `src/main/java/com/ratelimiter/algorithms/TokenBucketExecutor.java`, `src/test/java/com/ratelimiter/algorithms/TokenBucketExecutorTest.java`

---

### STORY-007 · Implement Sliding Window Algorithm

**Goal:** Implement the Sliding Window algorithm using a Redis sorted set to track exact request timestamps within a rolling time window.

**Context:** Unlike a fixed window counter, the sliding window never allows a burst at the boundary between two windows. Each request adds a timestamped entry; the Lua script removes stale entries and counts current ones atomically in a single EVAL call.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/algorithms/SlidingWindowExecutor.java`:
   - Redis key: `rl:sw:{policy.id}:{key}` — a Redis Sorted Set
   - Lua script:
```lua
-- script: sliding_window
local key         = KEYS[1]
local now         = tonumber(ARGV[1])
local windowMs    = tonumber(ARGV[2])
local limit       = tonumber(ARGV[3])
local ttlSeconds  = tonumber(ARGV[4])
local windowStart = now - windowMs

redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
local count = redis.call('ZCARD', key)

if count < limit then
  local member = now .. '-' .. math.random(100000)
  redis.call('ZADD', key, now, member)
  redis.call('EXPIRE', key, ttlSeconds)
  return {1, limit - count - 1, math.ceil((now + windowMs) / 1000)}
else
  local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
  local retryAfter = math.ceil((tonumber(oldest[2]) + windowMs - now) / 1000)
  return {0, 0, retryAfter}
end
```
   - Pass `args = List.of(nowMs, windowMs, limit, ttlSeconds)` where `ttlSeconds = ceil(windowMs / 1000)`
   - Parse result list into `RateLimitResult`

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- Exactly `limit` requests succeed within one window; `limit+1`th is rejected
- `retryAfter` is always > 0 when `allowed=false`
- After the window slides past the oldest request, exactly one new slot opens
- Unit tests cover: fill to limit, reject at limit+1, window slide re-opens slot

**Files to create:** `src/main/java/com/ratelimiter/algorithms/SlidingWindowExecutor.java`, `src/test/java/com/ratelimiter/algorithms/SlidingWindowExecutorTest.java`

---

### STORY-008 · Implement Leaky Bucket, Algorithm Factory & Core Service

**Goal:** Implement the Leaky Bucket algorithm, create a factory for algorithm instantiation, and assemble the core `RateLimiterService` that ties everything together.

**Context:** Leaky bucket enforces a uniform output rate — bursts up to `capacity` are accepted but processed at `leakRate` req/sec. Implemented by reusing token bucket mechanics. The factory and core service are the final pieces before moving to the gRPC layer.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/algorithms/LeakyBucketExecutor.java`:
   - Delegates to `TokenBucketExecutor` with `refillRate = policy.leakRate()`
   - Redis key prefix: `rl:lb:{policy.id}:{key}`
   - Override `retryAfter`: `(int) Math.ceil((double)(policy.capacity() - remaining) / policy.leakRate())`

2. Create `src/main/java/com/ratelimiter/algorithms/AlgorithmFactory.java`:
```java
public final class AlgorithmFactory {
    public static AlgorithmExecutor create(Algorithm algorithm) {
        return switch (algorithm) {
            case TOKEN_BUCKET   -> new TokenBucketExecutor();
            case SLIDING_WINDOW -> new SlidingWindowExecutor();
            case LEAKY_BUCKET   -> new LeakyBucketExecutor();
        };
    }
}
```
   Note: Java 21 switch expressions with sealed enums are exhaustive — no default needed.

3. Create `src/main/java/com/ratelimiter/RateLimiterService.java`:
```java
public class RateLimiterService {

    public RateLimiterService(
        PolicyLoader policyLoader,
        StoreAdapter store,
        FailMode failMode,
        BiConsumer<RateLimitRequest, RateLimitResult> onThrottle  // nullable
    ) { ... }

    public RateLimitResult check(RateLimitRequest request) { ... }
}
```
   - `check()` resolves policy → creates executor via factory → calls `executor.check()`
   - On `StoreException`:
     - `OPEN`: return `new RateLimitResult(true, 0, -1, 0, 0)` — signals degraded mode
     - `CLOSED`: throw `RateLimiterUnavailableException`
   - Call `onThrottle.accept(request, result)` if result is denied and `onThrottle != null`

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- `AlgorithmFactory.create()` returns the correct executor for each enum value
- Switch expression is exhaustive — adding a new `Algorithm` enum value causes a compile error
- `RateLimiterService.check()` returns correct results for allow and deny
- `StoreException` + `OPEN` → `RateLimitResult(allowed=true, remaining=-1)`
- `StoreException` + `CLOSED` → throws `RateLimiterUnavailableException`
- `onThrottle` invoked exactly once per rejected request
- Unit tests use Mockito-mocked store and policy loader

**Files to create:** `src/main/java/com/ratelimiter/algorithms/LeakyBucketExecutor.java`, `src/main/java/com/ratelimiter/algorithms/AlgorithmFactory.java`, `src/main/java/com/ratelimiter/RateLimiterService.java`, `src/test/java/com/ratelimiter/algorithms/AlgorithmFactoryTest.java`, `src/test/java/com/ratelimiter/RateLimiterServiceTest.java`

---

## Phase 3 — gRPC Server

---

### STORY-009 · Implement gRPC Service Handler

**Goal:** Implement the three gRPC RPC methods, mapping between proto types and internal domain models at the boundary.

**Context:** This class is the only place in the codebase where proto-generated types are used. All business logic lives in `RateLimiterService` and `PolicyLoader`. The handler translates, delegates, and maps errors to correct gRPC status codes.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/grpc/ProtoMapper.java`:
   - `RateLimitRequest  toRequest(CheckRateLimitRequest proto)`
   - `CheckRateLimitResponse toProto(RateLimitResult result)`
   - `RateLimitPolicy   toPolicy(PolicyConfig proto)`
   - `List<RateLimitPolicy> toPolicies(List<PolicyConfig> protos)`

2. Create `src/main/java/com/ratelimiter/grpc/RateLimiterGrpcHandler.java` extending `RateLimiterServiceGrpc.RateLimiterServiceImplBase`:

```java
@Override
public void checkRateLimit(CheckRateLimitRequest request,
                           StreamObserver<CheckRateLimitResponse> observer) {
    try {
        var result = rateLimiterService.check(ProtoMapper.toRequest(request));
        observer.onNext(ProtoMapper.toProto(result));
        observer.onCompleted();
    } catch (PolicyNotFoundException e) {
        observer.onError(Status.NOT_FOUND
            .withDescription(e.getMessage()).asException());
    } catch (RateLimiterUnavailableException e) {
        observer.onError(Status.UNAVAILABLE
            .withDescription(e.getMessage()).asException());
    }
}

@Override
public void reloadConfig(ReloadConfigRequest request,
                         StreamObserver<ReloadConfigResponse> observer) { ... }

@Override
public void healthCheck(HealthCheckRequest request,
                        StreamObserver<HealthCheckResponse> observer) { ... }
```

3. `reloadConfig`: call `policyLoader.reload(ProtoMapper.toPolicies(request.getPoliciesList()))`. On `ConfigValidationException` → `Status.INVALID_ARGUMENT` with all error messages joined.

4. `healthCheck`: call `store.ping()` and return `status="ok"` or `"degraded"` accordingly.

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- All three RPC methods are implemented
- `ProtoMapper` is tested in isolation — round-trip tests for each mapping
- Correct gRPC status codes: `NOT_FOUND`, `INVALID_ARGUMENT`, `UNAVAILABLE`
- Unit tests mock `RateLimiterService` and `PolicyLoader` with Mockito
- `observer.onCompleted()` is always called on the success path

**Files to create:** `src/main/java/com/ratelimiter/grpc/ProtoMapper.java`, `src/main/java/com/ratelimiter/grpc/RateLimiterGrpcHandler.java`, `src/test/java/com/ratelimiter/grpc/ProtoMapperTest.java`, `src/test/java/com/ratelimiter/grpc/RateLimiterGrpcHandlerTest.java`

---

### STORY-010 · Bootstrap gRPC Server & Wiring

**Goal:** Wire all components together into a running gRPC server with virtual thread executor, graceful shutdown, and environment-variable configuration.

**Context:** This is the composition root. Every component is instantiated here. The server must use Java 21 virtual threads as the executor so each gRPC call runs on a virtual thread — this gives us cheap concurrency without a thread pool bottleneck.

**Instructions:**

1. Update `src/main/java/com/ratelimiter/Main.java`:
```java
public class Main {
    public static void main(String[] args) throws Exception {
        String redisUri  = System.getenv().getOrDefault("REDIS_URI",    "redis://localhost:6379");
        int    grpcPort  = Integer.parseInt(System.getenv().getOrDefault("GRPC_PORT", "50051"));
        FailMode failMode = FailMode.valueOf(
            System.getenv().getOrDefault("FAIL_MODE", "OPEN"));

        var store         = new RedisStoreAdapter(redisUri);
        var policyLoader  = new PolicyLoader();
        var rateLimiter   = new RateLimiterService(policyLoader, store, failMode, null);
        var grpcHandler   = new RateLimiterGrpcHandler(rateLimiter, policyLoader, store);

        // Use virtual thread executor — each RPC call gets a virtual thread
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        var server = ServerBuilder.forPort(grpcPort)
            .executor(executor)
            .addService(ServerInterceptors.intercept(
                grpcHandler,
                new LoggingInterceptor(),
                new DeadlineInterceptor()))
            .build()
            .start();

        logger.info("gRPC server started on port {}", grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            server.shutdown();
            store.close();
            executor.shutdown();
        }));

        server.awaitTermination();
    }
}
```

2. Create `src/main/java/com/ratelimiter/grpc/LoggingInterceptor.java` implementing `ServerInterceptor`:
   - Log every inbound RPC: method name, peer address
   - Log every response: method name, gRPC status code, duration ms

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- `./gradlew run` starts and logs `"gRPC server started on port 50051"`
- `GRPC_PORT=9000 ./gradlew run` starts on port 9000
- Shutdown hook logs `"Shutting down..."` on `CTRL+C`
- Server uses virtual thread executor (assert `Thread.currentThread().isVirtual()` inside a test RPC handler)
- Smoke test in `ServerSmokeTest.java` starts server on a random port, calls `HealthCheck` via in-process channel, asserts `status="ok"`

**Files to create:** `src/main/java/com/ratelimiter/Main.java` (update), `src/main/java/com/ratelimiter/grpc/LoggingInterceptor.java`, `src/test/java/com/ratelimiter/ServerSmokeTest.java`

---

## Phase 4 — Observability

---

### STORY-011 · Implement Prometheus Metrics

**Goal:** Instrument the service with Micrometer counters, timers, and gauges, and expose them on a `/metrics` HTTP endpoint.

**Context:** The gRPC port does not serve HTTP. Add a minimal embedded HTTP server (Java built-in `HttpServer`) on a separate port for Prometheus scraping. Metrics must never block the gRPC request path — recording is synchronous but fast (in-memory counters).

**Instructions:**

1. Create `src/main/java/com/ratelimiter/metrics/MetricsRegistry.java`:
   - Use `PrometheusMeterRegistry`
   - Define meters:
     - `drl.requests.total` — `Counter`, tags: `algorithm`, `policy_id`, `result` (`allowed`/`throttled`)
     - `drl.store.latency` — `Timer`, tags: `operation` (`get`/`set`/`eval`)
     - `drl.store.errors.total` — `Counter`, tags: `operation`
     - `drl.bucket.fill.ratio` — `Gauge`, tags: `policy_id` (token bucket only, value 0.0–1.0)
   - Public methods:
     - `void recordRequest(String policyId, Algorithm algorithm, boolean allowed)`
     - `void recordStoreOp(String operation, long durationMs)`
     - `void recordStoreError(String operation)`
     - `String scrape()` — returns Prometheus text format

2. Create `src/main/java/com/ratelimiter/metrics/MetricsHttpServer.java`:
   - Use `com.sun.net.httpserver.HttpServer` (built into JDK — no extra dependency)
   - Port from env var `METRICS_PORT` (default: `9090`)
   - `GET /metrics` → `200 OK`, `Content-Type: text/plain; version=0.0.4`, body = `registry.scrape()`
   - `GET /healthz`  → `200 OK`, `Content-Type: application/json`, body = `{"status":"ok"}`
   - Use virtual thread executor: `server.setExecutor(Executors.newVirtualThreadPerTaskExecutor())`
   - `start()` and `stop()` methods for lifecycle management

3. Update `RateLimiterService.check()` to call `metricsRegistry.recordRequest(...)` after every evaluation (pass `MetricsRegistry` as optional constructor argument, nullable)

4. Update `RedisStoreAdapter` to accept optional `MetricsRegistry` and record latency/errors around `eval`, `get`, `set`

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- After 10 allowed + 5 throttled requests, `drl_requests_total{result="allowed"}` = 10 and `{result="throttled"}` = 5
- `curl http://localhost:9090/metrics` returns valid Prometheus text format
- `curl http://localhost:9090/healthz` returns `{"status":"ok"}`
- Metrics server runs on its own virtual thread pool — verified in unit test
- Unit tests use `SimpleMeterRegistry` (not Prometheus) to avoid scrape format dependency

**Files to create:** `src/main/java/com/ratelimiter/metrics/MetricsRegistry.java`, `src/main/java/com/ratelimiter/metrics/MetricsHttpServer.java`, `src/test/java/com/ratelimiter/metrics/MetricsRegistryTest.java`

---

### STORY-012 · Implement Structured Logging

**Goal:** Emit structured JSON log entries for every rate limit decision using SLF4J + Logback with `logstash-logback-encoder`.

**Context:** Logs must be machine-parseable JSON for ingestion into Datadog, Loki, or similar. Every `CheckRateLimit` call must produce a structured entry with enough context to debug incidents without querying Redis. Log level: `INFO` for allowed, `WARN` for throttled.

**Instructions:**

1. Create `src/main/resources/logback.xml`:
```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeContext>false</includeContext>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

2. Update `RateLimiterGrpcHandler.checkRateLimit()` to log after every decision:
```java
// Use logstash Markers to add structured fields alongside the message
import static net.logstash.logback.marker.Markers.append;

var marker = append("policyId",   request.getPolicyId())
    .and(append("key",        request.getKey()))
    .and(append("path",       request.getPath()))
    .and(append("method",     request.getMethod()))
    .and(append("allowed",    result.allowed()))
    .and(append("limit",      result.limit()))
    .and(append("remaining",  result.remaining()))
    .and(append("resetAt",    result.resetAt()))
    .and(append("latencyMs",  latencyMs));

if (result.allowed()) {
    log.info(marker, "rate_limit_check");
} else {
    log.warn(marker.and(append("retryAfter", result.retryAfter())), "rate_limit_throttled");
}
```
   - Measure `latencyMs` as wall-clock time from start to end of `checkRateLimit()`

3. Create `src/test/resources/logback-test.xml` with root level `ERROR` to silence logs during tests

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- Each log line is valid JSON with all required fields present
- Throttled requests log at `WARN` level with `retryAfter` field
- Allowed requests log at `INFO` level without `retryAfter` field
- `latencyMs` is always a non-negative number
- Zero console output during `./gradlew test`

**Files to create:** `src/main/resources/logback.xml`, `src/test/resources/logback-test.xml`, updated `RateLimiterGrpcHandler.java`

---

## Phase 5 — Resilience

---

### STORY-013 · Implement Circuit Breaker

**Goal:** Wrap `StoreAdapter` with a circuit breaker that trips after repeated failures and recovers automatically after a cooldown period.

**Context:** Without a circuit breaker, a slow or unavailable Redis causes every rate limit check to wait for a network timeout before failing. The circuit breaker detects repeated failures and short-circuits subsequent calls immediately, allowing `failMode=OPEN` to kick in with near-zero latency during outages.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/store/CircuitState.java`:
```java
public enum CircuitState { CLOSED, OPEN, HALF_OPEN }
```

2. Create `src/main/java/com/ratelimiter/store/CircuitBreakerStoreAdapter.java`:
   - Wraps any `StoreAdapter`
   - Configuration (with defaults):
     - `int failureThreshold = 5`
     - `int successThreshold = 2`
     - `long cooldownMs = 10_000`
   - State machine (use `AtomicReference<CircuitState>` and `AtomicInteger` counters — no `synchronized`):
     - `CLOSED` → `OPEN` after `failureThreshold` consecutive failures
     - `OPEN` → `HALF_OPEN` after `cooldownMs` elapses (check with `System.currentTimeMillis()`)
     - `HALF_OPEN` → `CLOSED` after `successThreshold` consecutive successes
     - `HALF_OPEN` → `OPEN` (reset timer) on any failure
   - When `OPEN`: throw `StoreException("Circuit open — store unavailable")` immediately, underlying adapter is NOT called
   - `public CircuitState getState()` — for observability
   - `public void reset()` — forces state back to `CLOSED` (for runbook manual recovery)

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- After 5 failures, `getState()` returns `OPEN`
- While `OPEN`, underlying adapter methods are never called (verify with `Mockito.verify(mock, never())`)
- After `cooldownMs`, state becomes `HALF_OPEN`
- 2 successes in `HALF_OPEN` → `CLOSED`
- Any failure in `HALF_OPEN` → `OPEN` with cooldown reset
- All transitions tested with `Mockito` and manual time manipulation (inject a `LongSupplier` clock)

**Files to create:** `src/main/java/com/ratelimiter/store/CircuitState.java`, `src/main/java/com/ratelimiter/store/CircuitBreakerStoreAdapter.java`, `src/test/java/com/ratelimiter/store/CircuitBreakerStoreAdapterTest.java`

---

### STORY-014 · Implement Deadline Interceptor

**Goal:** Add a gRPC server interceptor that enforces caller deadlines and rejects requests whose deadline has already expired before processing begins.

**Context:** Callers set deadlines on their gRPC calls (e.g., 50ms timeout). If Redis is slow, the rate limiter must not hold the caller past their deadline. This interceptor checks the deadline before touching the store and returns `DEADLINE_EXCEEDED` immediately if there is insufficient time budget remaining.

**Instructions:**

1. Create `src/main/java/com/ratelimiter/grpc/DeadlineInterceptor.java` implementing `ServerInterceptor`:
   - For each inbound call, check `Context.current().getDeadline()`
   - If deadline is `null`: proceed normally (no deadline set by caller)
   - If deadline has already passed (`deadline.isExpired()`): close immediately with `Status.DEADLINE_EXCEEDED`
   - If time remaining < `MIN_BUDGET_MS` (default `5`): close with `Status.DEADLINE_EXCEEDED`
   - Log all deadline-exceeded events at `WARN` level with method name and peer address
   - Use `io.grpc.Deadline` API for all time calculations

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- Expired deadline → `DEADLINE_EXCEEDED` returned immediately, underlying service method is NOT invoked
- Deadline with > 5ms remaining → call proceeds normally
- No deadline set → call proceeds normally
- Deadline-exceeded events appear in logs at `WARN`
- Unit test uses an in-process gRPC channel to send calls with pre-expired deadlines

**Files to create:** `src/main/java/com/ratelimiter/grpc/DeadlineInterceptor.java`, `src/test/java/com/ratelimiter/grpc/DeadlineInterceptorTest.java`

---

## Phase 6 — Hardening

---

### STORY-015 · Dockerize the Service

**Goal:** Package the service as a minimal Docker image and provide a `docker-compose.yml` for local development with Redis.

**Instructions:**

1. Configure `shadowJar` in `build.gradle` for a fat JAR:
```groovy
plugins {
    id "com.github.johnrengelman.shadow" version "8.1.1"
}
shadowJar {
    archiveClassifier = ""
    manifest {
        attributes "Main-Class": "com.ratelimiter.Main"
    }
}
```

2. Create a multi-stage `Dockerfile`:
```dockerfile
# Stage 1 — Build
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

# Stage 2 — Runtime (minimal JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 50051 9090
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
```

3. Create `docker-compose.yml`:
```yaml
version: "3.9"
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  rate-limiter:
    build: .
    ports:
      - "50051:50051"
      - "9090:9090"
    environment:
      REDIS_URI:    redis://redis:6379
      GRPC_PORT:    "50051"
      METRICS_PORT: "9090"
      FAIL_MODE:    OPEN
    depends_on: [redis]
    healthcheck:
      test: ["CMD", "grpc_health_probe", "-addr=:50051"]
      interval: 10s
      timeout: 5s
      retries: 3
```

4. Create `docker-compose.test.yml` for integration tests (Redis only, separate port):
```yaml
version: "3.9"
services:
  redis-test:
    image: redis:7-alpine
    ports: ["6399:6379"]
```

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- `docker build -t rate-limiter .` succeeds, image size < 200MB
- `docker-compose up` starts Redis and rate-limiter, service is healthy
- `grpcurl -plaintext localhost:50051 ratelimiter.v1.RateLimiterService/HealthCheck` returns `{"status":"ok","storeOk":true}`
- `curl localhost:9090/healthz` returns `{"status":"ok"}`
- `docker-compose down` cleanly stops both services

**Files to create:** `Dockerfile`, `docker-compose.yml`, `docker-compose.test.yml`, updated `build.gradle`

---

### STORY-016 · Write Integration Tests with Testcontainers

**Goal:** Validate the full stack end-to-end using a real Redis instance managed by Testcontainers.

**Context:** Unit tests use mocks. Integration tests prove that Lua scripts execute correctly on real Redis, TTL expiry works, concurrent requests are handled without over-admission, and gRPC endpoints return correct responses.

**Instructions:**

1. Create `src/test/java/com/ratelimiter/integration/IntegrationTestBase.java`:
   - Annotate with `@Testcontainers`
   - Static `@Container GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379)`
   - Start gRPC server on random port using `InProcessServerBuilder`
   - Wire real `RedisStoreAdapter` pointing at Testcontainers Redis
   - `@AfterEach` calls `FLUSHDB` to clean state between tests

2. Write these test classes extending `IntegrationTestBase`:
   - `TokenBucketIntegrationTest.java` — 50 concurrent virtual threads hit the same key; assert exactly `capacity` are allowed (use `CountDownLatch` and `AtomicInteger`)
   - `SlidingWindowIntegrationTest.java` — fill window to `limit`, assert rejection, sleep `windowMs`, assert one slot reopens
   - `LeakyBucketIntegrationTest.java` — assert rate enforcement: `leakRate` req/sec over a 2-second window
   - `GrpcEndpointIntegrationTest.java` — call each RPC method via a real gRPC stub:
     - `CheckRateLimit` → assert `allowed=true` then throttled after limit
     - `ReloadConfig` → change limit, assert new limit is enforced on next call
     - `HealthCheck` → assert `storeOk=true`

3. Configure a separate `integrationTest` Gradle source set and task:
```groovy
sourceSets { integrationTest { ... } }
tasks.register("integrationTest", Test) {
    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath
    useJUnitPlatform()
}
```

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- All integration tests pass with Testcontainers Redis
- Concurrency test never admits more than `capacity` requests regardless of thread interleaving
- `ReloadConfig` integration test confirms updated limit is active on the very next `CheckRateLimit` call
- `./gradlew integrationTest` runs separately from `./gradlew test`
- CI has a separate `integration` job that runs after the `test` job

**Files to create:** `src/test/java/com/ratelimiter/integration/IntegrationTestBase.java`, `src/test/java/com/ratelimiter/integration/TokenBucketIntegrationTest.java`, `src/test/java/com/ratelimiter/integration/SlidingWindowIntegrationTest.java`, `src/test/java/com/ratelimiter/integration/LeakyBucketIntegrationTest.java`, `src/test/java/com/ratelimiter/integration/GrpcEndpointIntegrationTest.java`, updated `build.gradle`

---

### STORY-017 · Write Public Documentation

**Goal:** Produce complete operator and developer documentation covering deployment, configuration, gRPC API reference, and an operations runbook.

**Instructions:**

Create a `docs/` folder with the following five files:

1. **`docs/README.md`** — Overview and quick start:
   - What the service does in 3 sentences
   - Quick start: `docker-compose up` + one working `grpcurl` command for each of the 3 RPC methods
   - Links to all other docs pages

2. **`docs/configuration.md`** — All configuration options:
   - Environment variables table: `REDIS_URI`, `GRPC_PORT`, `METRICS_PORT`, `FAIL_MODE` — with type, default, and description for each
   - Complete `policies.yaml` field reference with types, which algorithm each field applies to, and valid value ranges
   - How to use `ReloadConfig` gRPC call for live updates with a `grpcurl` example

3. **`docs/api.md`** — gRPC API reference:
   - All three RPC methods: request fields, response fields, description of each
   - gRPC status codes the service returns and the exact condition that triggers each
   - Working `grpcurl` command examples for every endpoint with sample request JSON
   - Response field semantics (`allowed`, `remaining`, `resetAt`, `retryAfter`)

4. **`docs/algorithms.md`** — Algorithm selection guide:
   - Plain-English explanation of each algorithm with an analogy
   - Decision table: which algorithm to pick for: public API (bursty traffic), payment processing (smooth rate), per-minute quotas (strict window)
   - Full `policies.yaml` config example for each algorithm
   - Memory and performance trade-offs per algorithm

5. **`docs/operations.md`** — Runbook:
   - Circuit breaker state machine diagram in ASCII
   - How to manually reset the circuit breaker (via `reset()` — document how to trigger this in production)
   - Fail-open vs fail-closed: when to choose each with concrete examples
   - Incident runbooks for: Redis outage, throttle rate spike, misconfigured policy deployed, service OOM
   - Prometheus alerting rules as copy-pasteable YAML for: throttle rate > 20%, store error rate > 1%, p99 latency > 10ms

**Acceptance Criteria:**
- Add or update unit tests that cover this story's scope.
- Mark this story complete only after `./gradlew test` passes.
- Every `grpcurl` command in `docs/api.md` is syntactically correct and runnable against a live service
- All environment variables are documented with their exact default values
- Algorithm decision table covers at least 3 named use cases
- All 4 incident scenarios in the runbook have at least: symptoms, diagnosis steps, and resolution steps

**Files to create:** `docs/README.md`, `docs/configuration.md`, `docs/api.md`, `docs/algorithms.md`, `docs/operations.md`

---

## Dependency Map

```
001 (Gradle scaffold)
 └─ 002 (proto contract)
     └─ 003 (domain model)
         ├─ 004 (store abstraction)
         └─ 005 (config loader)
             ├─ 006 (token bucket)      ← needs 004, 005
             ├─ 007 (sliding window)    ← needs 004, 005
             └─ 008 (leaky bucket + factory + core service)  ← needs 006, 007
                 └─ 009 (gRPC handler)  ← needs 008
                     └─ 010 (server wiring + virtual threads)
                         ├─ 011 (Prometheus metrics)
                         ├─ 012 (structured logging)
                         ├─ 013 (circuit breaker)   ← needs 004
                         └─ 014 (deadline interceptor) ← needs 009
                             └─ 015 (Docker)
                                 └─ 016 (integration tests)
                                     └─ 017 (documentation)
```

---

## Summary Table

| Story | Phase | Title | Key Java 21 Feature Used |
|---|---|---|---|
| STORY-001 | Foundation | Gradle Project Setup | Java toolchain API |
| STORY-002 | Foundation | Protobuf Contract | — |
| STORY-003 | Foundation | Domain Model | Records, sealed enums |
| STORY-004 | Foundation | Redis Store Abstraction | Virtual threads, `StructuredTaskScope` |
| STORY-005 | Foundation | Config Loader | Records, `volatile` swap |
| STORY-006 | Algorithms | Token Bucket | — |
| STORY-007 | Algorithms | Sliding Window | — |
| STORY-008 | Algorithms | Leaky Bucket + Factory + Core Service | Exhaustive switch expressions |
| STORY-009 | gRPC Server | gRPC Handler | — |
| STORY-010 | gRPC Server | Server Bootstrap | `Executors.newVirtualThreadPerTaskExecutor()` |
| STORY-011 | Observability | Prometheus Metrics | Virtual thread HTTP server |
| STORY-012 | Observability | Structured Logging | — |
| STORY-013 | Resilience | Circuit Breaker | `AtomicReference`, injectable clock |
| STORY-014 | Resilience | Deadline Interceptor | — |
| STORY-015 | Hardening | Dockerize | eclipse-temurin:21-jre-alpine |
| STORY-016 | Hardening | Integration Tests | Virtual threads in concurrency test |
| STORY-017 | Hardening | Documentation | — |
