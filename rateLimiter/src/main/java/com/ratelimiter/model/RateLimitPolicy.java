package com.ratelimiter.model;

public record RateLimitPolicy(
        String id,
        Algorithm algorithm,
        int limit,
        long windowMs,
        int refillRate,
        int capacity,
        int leakRate
) {
    public RateLimitPolicy {
        if (windowMs <= 0) {
            windowMs = 60_000L;
        }
        if (refillRate <= 0) {
            refillRate = 10;
        }
        if (capacity <= 0) {
            capacity = 100;
        }
        if (leakRate <= 0) {
            leakRate = 10;
        }
    }
}
