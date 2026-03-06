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
import java.util.concurrent.atomic.AtomicReference;

public class MetricsRegistry {
    private final MeterRegistry meterRegistry;
    private final PrometheusMeterRegistry prometheusRegistry;
    private final Map<String, AtomicReference<Double>> bucketFillRatios = new ConcurrentHashMap<>();

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
        AtomicReference<Double> ratioRef = bucketFillRatios.computeIfAbsent(policyId, id -> {
            AtomicReference<Double> ref = new AtomicReference<>(0.0d);
            Gauge.builder("drl.bucket.fill.ratio", ref, AtomicReference::get)
                    .tag("policy_id", id)
                    .register(meterRegistry);
            return ref;
        });
        ratioRef.set(ratio);
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
