package com.ratelimiter;

import com.ratelimiter.algorithms.AlgorithmExecutor;
import com.ratelimiter.algorithms.AlgorithmFactory;
import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.metrics.MetricsRegistry;
import com.ratelimiter.model.Errors.RateLimiterUnavailableException;
import com.ratelimiter.model.Errors.StoreException;
import com.ratelimiter.model.FailMode;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;
import java.util.function.BiConsumer;

public class RateLimiterService {
    private final PolicyLoader policyLoader;
    private final StoreAdapter store;
    private final FailMode failMode;
    private final BiConsumer<RateLimitRequest, RateLimitResult> onThrottle;
    private final MetricsRegistry metricsRegistry;

    public RateLimiterService(
            PolicyLoader policyLoader,
            StoreAdapter store,
            FailMode failMode,
            BiConsumer<RateLimitRequest, RateLimitResult> onThrottle
    ) {
        this(policyLoader, store, failMode, onThrottle, null);
    }

    public RateLimiterService(
            PolicyLoader policyLoader,
            StoreAdapter store,
            FailMode failMode,
            BiConsumer<RateLimitRequest, RateLimitResult> onThrottle,
            MetricsRegistry metricsRegistry
    ) {
        this.policyLoader = policyLoader;
        this.store = store;
        this.failMode = failMode;
        this.onThrottle = onThrottle;
        this.metricsRegistry = metricsRegistry;
    }

    public RateLimitResult check(RateLimitRequest request) {
        RateLimitPolicy policy = policyLoader.getPolicy(request.policyId());
        AlgorithmExecutor executor = AlgorithmFactory.create(policy.algorithm());

        RateLimitResult result;
        try {
            result = executor.check(request.key(), policy, store);
        } catch (StoreException e) {
            if (failMode == FailMode.OPEN) {
                return new RateLimitResult(true, 0, -1, 0, 0);
            }
            throw new RateLimiterUnavailableException("Rate limiter unavailable", e);
        }

        if (!result.allowed() && onThrottle != null) {
            onThrottle.accept(request, result);
        }
        if (metricsRegistry != null) {
            metricsRegistry.recordRequest(policy.id(), policy.algorithm(), result.allowed());
        }

        return result;
    }
}
