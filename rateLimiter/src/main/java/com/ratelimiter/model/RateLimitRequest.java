package com.ratelimiter.model;

public record RateLimitRequest(String policyId, String key, String path, String method) {
}
