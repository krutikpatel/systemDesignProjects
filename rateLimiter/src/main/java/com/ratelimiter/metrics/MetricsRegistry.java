package com.ratelimiter.metrics;

import com.ratelimiter.model.Algorithm;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsRegistry {
    private final MeterRegistry meterRegistry;
    private final PrometheusMeterRegistry prometheusRegistry;
    private final Map<String, Double> bucketFillRatios = new ConcurrentHashMap<>();

    public MetricsRegistry() {
        this(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
    }

    public MetricsRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.prometheusRegistry = meterRegistry instanceof PrometheusMeterRegistry p ? p : null;
    }

    public void recordRequest(String policyId, Algorithm algorithm, boolean allowed) {
        String result = allowed ? "allowed" : "throttled";
        Counter.builder("drl.requests.total")
                .tag("algorithm", algorithm.name())
                .tag("policy_id", policyId)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void recordStoreOp(String operation, long durationMs) {
        Timer.builder("drl.store.latency")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    public void recordStoreError(String operation) {
        Counter.builder("drl.store.errors.total")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }

    public void recordBucketFillRatio(String policyId, double ratio) {
        bucketFillRatios.put(policyId, ratio);
        Gauge.builder("drl.bucket.fill.ratio", bucketFillRatios, map -> map.getOrDefault(policyId, 0.0d))
                .tag("policy_id", policyId)
                .register(meterRegistry);
    }

    public String scrape() {
        if (prometheusRegistry != null) {
            return prometheusRegistry.scrape();
        }
        return "# SimpleMeterRegistry does not support Prometheus scrape";
    }

    MeterRegistry meterRegistry() {
        return meterRegistry;
    }

    static MetricsRegistry simple() {
        return new MetricsRegistry(new SimpleMeterRegistry());
    }
}
