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
}
