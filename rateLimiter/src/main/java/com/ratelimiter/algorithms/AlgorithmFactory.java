package com.ratelimiter.algorithms;

import com.ratelimiter.model.Algorithm;

public final class AlgorithmFactory {
    private AlgorithmFactory() {
    }

    public static AlgorithmExecutor create(Algorithm algorithm) {
        return switch (algorithm) {
            case TOKEN_BUCKET -> new TokenBucketExecutor();
            case SLIDING_WINDOW -> new SlidingWindowExecutor();
            case LEAKY_BUCKET -> new LeakyBucketExecutor();
        };
    }
}
