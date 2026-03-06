---
title: "Distributed Rate Limiter — Project Plan & Technical Specification"
version: "1.0"
date: "March 2026"
---

# DISTRIBUTED RATE LIMITER

Project Plan & Technical Specification

*API Gateway / Microservices Edition*

Version 1.0 \| March 2026

  -----------------------------------------------------------------------
  **Attribute**          **Detail**
  ---------------------- ------------------------------------------------
  Project Name           Distributed Rate Limiter (DRL)

  Primary Use Case       API Gateway / Microservices

  Target Algorithms      Token Bucket, Sliding Window, Leaky Bucket

  Preferred Store        Undecided (Redis recommended, Memcached as alt)

  Document Status        Draft --- For Review
  -----------------------------------------------------------------------

## 1. Executive Summary
This document defines the project plan and detailed technical
specification for a Distributed Rate Limiter (DRL) module designed for
use in API gateway and microservices architectures. The system enforces
configurable traffic limits across horizontally-scaled service instances
with strong consistency guarantees, observable metrics, and
fault-tolerant behavior.

The DRL will support three core algorithms --- Token Bucket, Sliding
Window, and Leaky Bucket --- and expose both middleware and SDK
interfaces for seamless integration into existing gateway stacks.

## 2. Goals & Non-Goals
### 2.1 Goals
-   Enforce per-client, per-IP, and per-endpoint rate limits across
    multiple service nodes

-   Support Token Bucket, Sliding Window, and Leaky Bucket algorithms

-   Guarantee atomic, race-condition-free limit checks via distributed
    shared state

-   Provide a middleware interface compatible with major API gateway
    frameworks

-   Emit real-time metrics, logs, and alerts for observability

-   Handle backend store failures gracefully with configurable fail-open
    or fail-closed modes

-   Support dynamic rule updates without service restarts

### 2.2 Non-Goals
-   This module will not implement application-level business logic or
    authentication

-   Global DDoS mitigation at the network layer is out of scope

-   The system will not act as a full API gateway or reverse proxy

-   Persistent audit logging for compliance or billing is deferred to a
    future phase

## 3. System Architecture
### 3.1 High-Level Design
The DRL consists of four major subsystems: the Rule Engine, the
Algorithm Layer, the State Store Abstraction, and the Observability
Layer. All inbound requests pass through the Rule Engine to identify
which limit policy applies, then the Algorithm Layer evaluates whether
the request is allowed or should be throttled, reading and writing
atomic state from the distributed store.

  -----------------------------------------------------------------------
  **Component**    **Responsibility**              **Technology**
  ---------------- ------------------------------- ----------------------
  Rule Engine      Matches requests to limit       In-process config
                   policies by key, endpoint,      loader
                   method                          

  Algorithm Layer  Executes Token Bucket, Sliding  Go / Node.js module
                   Window, or Leaky Bucket logic   

  State Store      Atomic reads/writes; pluggable  Redis Cluster
  Abstraction      backend (Redis, Memcached)      (primary)

  Observability    Metrics, structured logging,    Prometheus + Grafana
  Layer            alerting                        

  Middleware       Integrates with Express,        Language-specific SDK
  Adapter          Fastify, Kong, Envoy, etc.      
  -----------------------------------------------------------------------

### 3.2 Request Lifecycle
Each request goes through the following stages:

1.  Middleware intercepts the inbound request before it reaches the
    service handler.

2.  The Rule Engine resolves the rate limit key (e.g., user ID, IP, API
    key) and retrieves the matching policy.

3.  The Algorithm Layer issues an atomic check-and-update to the State
    Store.

4.  If allowed: the request proceeds and response headers are enriched
    (X-RateLimit-\*).

5.  If throttled: HTTP 429 is returned immediately with a Retry-After
    header.

6.  Metrics and events are emitted asynchronously to the Observability
    Layer.

## 4. Algorithm Specifications
### 4.1 Token Bucket
The Token Bucket algorithm maintains a \'bucket\' with a maximum
capacity. Tokens are added at a fixed refill rate. Each request consumes
one token. If the bucket is empty, the request is rejected.

  ------------------------------------------------------------------------
  **Parameter**      **Description**                   **Default**
  ------------------ --------------------------------- -------------------
  capacity           Maximum number of tokens the      100
                     bucket can hold                   

  refill_rate        Tokens added per second           10

  initial_tokens     Tokens at bucket creation         Equal to capacity

  atomic_op          Lua script via EVAL for atomicity Required
  ------------------------------------------------------------------------

Best for: Bursty traffic with sustained average rate enforcement. Allows
short bursts up to capacity while smoothing long-term throughput.

### 4.2 Sliding Window
The Sliding Window algorithm tracks request timestamps within a rolling
time window. A request is allowed if the count of requests in the past
\[window_size\] seconds is below the limit. This avoids the boundary
spike problem of fixed window counters.

  ------------------------------------------------------------------------
  **Parameter**      **Description**                   **Default**
  ------------------ --------------------------------- -------------------
  window_size_ms     Duration of the sliding window in 60000 (1 min)
                     milliseconds                      

  max_requests       Maximum requests allowed in the   100
                     window                            

  storage_key        Sorted set key pattern:           Configurable
                     rl:{key}:{endpoint}               

  precision          Time granularity for bucket       1000ms sub-buckets
                     subdivision                       
  ------------------------------------------------------------------------

Best for: APIs requiring strict per-minute or per-hour limits with no
burst allowance. More memory-intensive but highly accurate.

### 4.3 Leaky Bucket
The Leaky Bucket algorithm queues incoming requests and processes them
at a fixed rate, \'leaking\' at a constant pace. Requests that exceed
the queue capacity are dropped immediately. This enforces a hard,
uniform output rate.

  ------------------------------------------------------------------------
  **Parameter**      **Description**                   **Default**
  ------------------ --------------------------------- -------------------
  rate               Requests processed per second     10
                     (leak rate)                       

  capacity           Maximum queue depth before        100
                     requests are dropped              

  implementation     Approximated via token bucket     Redis Lua
                     with fixed refill                 
  ------------------------------------------------------------------------

Best for: Downstream services with strict SLA requirements that cannot
tolerate bursty input (e.g., payment processors, third-party APIs).

## 5. Backend Store Design
### 5.1 Store Comparison
  ----------------------------------------------------------------------------------
  **Store**     **Atomicity**   **Cluster     **Persistence**   **Recommendation**
                                Support**                       
  ------------- --------------- ------------- ----------------- --------------------
  Redis         Lua EVAL        Yes (Redis    RDB + AOF         Primary choice
  (Cluster)     scripts         Cluster)      optional          

  Redis         Lua EVAL        HA via        RDB + AOF         Simpler HA option
  (Sentinel)    scripts         Sentinel      optional          

  Memcached     CAS operations  Limited       None              Fallback for
                only                                            existing infra

  In-Memory     Native atomics  No --- single None              Dev/test only
  (local)                       node only                       
  ----------------------------------------------------------------------------------

### 5.2 Redis Key Schema
All keys follow a hierarchical namespace to allow targeted expiry and
observability:

-   Token Bucket: rl:tb:{tenant}:{key}:{endpoint}

-   Sliding Window: rl:sw:{tenant}:{key}:{endpoint}

-   Leaky Bucket: rl:lb:{tenant}:{key}:{endpoint}

-   Config cache: rl:cfg:{policy_id}

### 5.3 Atomicity Strategy
All read-modify-write operations are implemented as Redis Lua scripts
executed via EVAL. This guarantees that the check and update happen
atomically on the Redis server without race conditions from concurrent
service instances. Scripts are SHA-cached via EVALSHA after first load.

## 6. API & Integration Specification
### 6.1 Middleware Interface
The DRL exposes a framework-agnostic middleware function with the
following signature:

rateLimiter(options: RateLimiterOptions): MiddlewareFunction

  --------------------------------------------------------------------------
  **Option**      **Type**       **Description**
  --------------- -------------- -------------------------------------------
  algorithm       string         \'token_bucket\' \| \'sliding_window\' \|
                                 \'leaky_bucket\'

  keyResolver     function       Extracts the limit key from the request
                                 (e.g., req.user.id)

  limit           number         Max requests allowed per window or bucket
                                 capacity

  window / rate   number         Window size (ms) or refill rate (req/sec)

  store           StoreAdapter   Pluggable store instance (Redis, Memcached,
                                 Memory)

  onThrottle      function       Optional hook called when a request is
                                 rejected

  failMode        string         \'open\' \| \'closed\' --- behavior when
                                 store is unavailable
  --------------------------------------------------------------------------

### 6.2 Response Headers
  -----------------------------------------------------------------------
  **Header**               **Description**
  ------------------------ ----------------------------------------------
  X-RateLimit-Limit        The maximum number of requests allowed

  X-RateLimit-Remaining    Requests remaining in the current window or
                           bucket

  X-RateLimit-Reset        Unix timestamp (seconds) when the limit resets

  Retry-After              Seconds to wait before retrying (included on
                           429 only)
  -----------------------------------------------------------------------

## 7. Observability
### 7.1 Metrics
  ----------------------------------------------------------------------------
  **Metric Name**          **Type**    **Description**
  ------------------------ ----------- ---------------------------------------
  drl_requests_total       Counter     Total requests evaluated by the limiter

  drl_throttled_total      Counter     Total requests rejected with 429

  drl_store_latency_ms     Histogram   Latency of store read/write operations

  drl_store_errors_total   Counter     Number of store errors (timeouts,
                                       connection failures)

  drl_bucket_fill_ratio    Gauge       Current fill ratio of a bucket (token
                                       bucket only)
  ----------------------------------------------------------------------------

### 7.2 Structured Logging
All throttle events are logged as structured JSON with the following
fields: timestamp, key, endpoint, algorithm, limit, remaining, result
(allowed/throttled), latency_ms, and store_backend. Log level is WARN
for throttled requests and INFO for allowed requests (configurable).

### 7.3 Alerting Rules
-   Alert if throttle rate exceeds 20% of total requests over a 5-minute
    window

-   Alert if store error rate exceeds 1% of requests over a 1-minute
    window

-   Alert if p99 store latency exceeds 10ms

## 8. Resilience & Fault Tolerance
  ------------------------------------------------------------------------
  **Failure          **Fail-Open Behavior** **Fail-Closed Behavior**
  Scenario**                                
  ------------------ ---------------------- ------------------------------
  Redis unreachable  Allow all requests,    Reject all requests with 503
                     emit metric alert      

  Redis timeout      Allow request, log     Reject with 503
  (\>5ms)            warning                

  Key expiry race    Lua script prevents;   N/A
  condition          handled atomically     

  Node restart       State persisted in     Same
                     Redis; no warm-up      
                     needed                 

  Clock skew across  Sliding window uses    Same
  nodes              server-side Redis time 
  ------------------------------------------------------------------------

The default fail mode is fail-open to prioritize availability. Operators
can switch to fail-closed for security-critical endpoints via per-policy
configuration.

## 9. Project Phases & Timeline
  -----------------------------------------------------------------------------
  **Phase**       **Milestone**          **Duration**   **Deliverables**
  --------------- ---------------------- -------------- -----------------------
  1 ---           Core architecture,     2 weeks        Store adapter
  Foundation      store abstraction,                    interface, Redis
                  Redis integration                     client, CI setup

  2 ---           Token Bucket, Sliding  3 weeks        Algorithm modules with
  Algorithms      Window, Leaky Bucket                  Lua scripts, unit tests
                  implementations                       

  3 ---           Framework adapters,    2 weeks        Express/Fastify
  Middleware      key resolvers,                        middleware, integration
                  response headers                      tests

  4 ---           Prometheus metrics,    1 week         Metrics exporter,
  Observability   structured logging,                   dashboard JSON, alert
                  Grafana dashboard                     rules

  5 ---           Failover logic,        1 week         Failover tests, config
  Resilience      circuit breaker,                      reload API
                  dynamic config reload                 

  6 --- Hardening Load testing,          2 weeks        Perf report, API docs,
                  performance tuning,                   runbook
                  documentation                         
  -----------------------------------------------------------------------------

Total estimated timeline: 11 weeks from kickoff to production-ready
release.

## 10. Risks & Mitigations
  -----------------------------------------------------------------------------------
  **Risk**           **Likelihood**   **Impact**   **Mitigation**
  ------------------ ---------------- ------------ ----------------------------------
  Redis becomes      Medium           High         Redis Cluster or Sentinel;
  single point of                                  fail-open fallback
  failure                                          

  Clock skew causing Low              Medium       Use Redis server TIME command, not
  incorrect windows                                client clock

  Lua script         Low              High         Short scripts; timeout budget;
  execution blocking                               async metrics
  Redis                                            

  Misconfigured      Medium           High         Config validation on load; dry-run
  limits causing                                   mode for testing rules
  outage                                           

  Backend store      Medium           Medium       Build against abstraction layer;
  choice delay                                     Redis as default
  blocking dev                                     
  -----------------------------------------------------------------------------------

## 11. Success Criteria
-   Rate limiting overhead adds less than 2ms p99 latency to any request

-   System correctly enforces limits with zero over-admission under
    concurrent load (verified via load test at 10k RPS)

-   Failover from Redis outage completes in under 500ms with no service
    crash

-   Dynamic rule update propagates to all nodes within 5 seconds without
    restart

-   Observability dashboard shows accurate throttle rates within
    15-second resolution

## 12. Open Questions
-   Backend store decision: Should Redis Cluster be mandated, or should
    Memcached support be built in Phase 1?

-   Multi-tenancy: Will each tenant have isolated Redis key namespaces,
    or shared with logical separation?

-   Quota top-ups: Is there a need for real-time quota adjustments
    (e.g., admin API to reset a user\'s bucket)?

-   SDK vs. sidecar: Should the DRL be embedded as a library or deployed
    as a sidecar/service mesh plugin?

-   Persistence: Should Redis AOF persistence be required, or is
    in-memory-only acceptable given fail-open behavior?

*Document Owner: Engineering Platform Team*

*Last Updated: March 2026*
