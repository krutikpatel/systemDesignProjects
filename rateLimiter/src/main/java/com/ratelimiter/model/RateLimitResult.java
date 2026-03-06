package com.ratelimiter.model;

public record RateLimitResult(boolean allowed, int limit, int remaining, long resetAt, int retryAfter) {
}
