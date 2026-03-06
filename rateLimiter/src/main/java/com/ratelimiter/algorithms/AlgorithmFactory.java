package com.ratelimiter.algorithms;

import com.ratelimiter.model.Algorithm;
import java.util.EnumMap;
import java.util.Map;

public final class AlgorithmFactory {
    private static final Map<Algorithm, AlgorithmExecutor> EXECUTORS = executors();

    private AlgorithmFactory() {
    }

    public static AlgorithmExecutor create(Algorithm algorithm) {
        AlgorithmExecutor executor = EXECUTORS.get(algorithm);
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        return executor;
    }

    private static Map<Algorithm, AlgorithmExecutor> executors() {
        EnumMap<Algorithm, AlgorithmExecutor> map = new EnumMap<>(Algorithm.class);
        map.put(Algorithm.TOKEN_BUCKET, new TokenBucketExecutor());
        map.put(Algorithm.SLIDING_WINDOW, new SlidingWindowExecutor());
        map.put(Algorithm.LEAKY_BUCKET, new LeakyBucketExecutor());
        return Map.copyOf(map);
    }
}
