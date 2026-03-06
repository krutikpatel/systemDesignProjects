package com.ratelimiter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TokenBucketIntegrationTest extends IntegrationTestBase {

    @Test
    void concurrentRequestsAllowOnlyCapacity() throws Exception {
        policyLoader.reload(java.util.List.of(
                new RateLimitPolicy("default", Algorithm.TOKEN_BUCKET, 10, 0, 1, 10, 0)
        ));

        int total = 50;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);
        AtomicInteger allowed = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < total; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        if (rateLimiterService.check(new RateLimitRequest("default", "same-key", "/", "GET")).allowed()) {
                            allowed.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await(5, TimeUnit.SECONDS);
        }

        assertEquals(10, allowed.get());
    }
}
