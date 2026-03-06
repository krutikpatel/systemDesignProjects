package com.ratelimiter.algorithms;

import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;

public interface AlgorithmExecutor {
    RateLimitResult check(String key, RateLimitPolicy policy, StoreAdapter store);
}
