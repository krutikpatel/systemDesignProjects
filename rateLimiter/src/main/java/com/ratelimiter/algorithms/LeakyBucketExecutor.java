package com.ratelimiter.algorithms;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;
import java.time.Instant;
import java.util.function.LongSupplier;

public class LeakyBucketExecutor implements AlgorithmExecutor {
    private final TokenBucketExecutor delegate;

    public LeakyBucketExecutor() {
        this(new TokenBucketExecutor(() -> Instant.now().getEpochSecond(), "rl:lb"));
    }

    LeakyBucketExecutor(LongSupplier nowSeconds) {
        this(new TokenBucketExecutor(nowSeconds, "rl:lb"));
    }

    LeakyBucketExecutor(TokenBucketExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy, StoreAdapter store) {
        RateLimitPolicy delegatedPolicy = new RateLimitPolicy(
                policy.id(),
                Algorithm.TOKEN_BUCKET,
                policy.limit(),
                policy.windowMs(),
                policy.leakRate(),
                policy.capacity(),
                policy.leakRate()
        );

        RateLimitResult result = delegate.check(key, delegatedPolicy, store);
        if (result.allowed()) {
            return result;
        }

        int retryAfter = (int) Math.ceil((double) (policy.capacity() - result.remaining()) / policy.leakRate());
        return new RateLimitResult(false, result.limit(), result.remaining(), result.resetAt(), retryAfter);
    }
}
