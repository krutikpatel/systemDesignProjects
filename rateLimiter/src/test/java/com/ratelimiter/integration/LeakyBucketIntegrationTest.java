package com.ratelimiter.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import org.junit.jupiter.api.Test;

class LeakyBucketIntegrationTest extends IntegrationTestBase {

    @Test
    void leakyBucketEnforcesRate() throws Exception {
        policyLoader.reload(java.util.List.of(
                new RateLimitPolicy("default", Algorithm.LEAKY_BUCKET, 10, 0, 0, 2, 2)
        ));

        assertTrue(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());
        assertTrue(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());
        assertFalse(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());

        Thread.sleep(600);
        assertTrue(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());
    }
}
