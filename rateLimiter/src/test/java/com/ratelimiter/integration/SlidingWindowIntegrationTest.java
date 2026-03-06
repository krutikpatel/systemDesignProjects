package com.ratelimiter.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import org.junit.jupiter.api.Test;

class SlidingWindowIntegrationTest extends IntegrationTestBase {

    @Test
    void slidingWindowRejectsThenAllowsAfterWindow() throws Exception {
        policyLoader.reload(java.util.List.of(
                new RateLimitPolicy("default", Algorithm.SLIDING_WINDOW, 2, 300, 0, 0, 0)
        ));

        assertTrue(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());
        assertTrue(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());
        assertFalse(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());

        Thread.sleep(350);
        assertTrue(rateLimiterService.check(new RateLimitRequest("default", "k", "/", "GET")).allowed());
    }
}
