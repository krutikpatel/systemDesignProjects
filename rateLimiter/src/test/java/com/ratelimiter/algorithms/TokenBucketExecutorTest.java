package com.ratelimiter.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.InMemoryStoreAdapter;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TokenBucketExecutorTest {

    @Test
    void firstRequestIsAlwaysAllowed() {
        AtomicLong clock = new AtomicLong(1_000);
        TokenBucketExecutor executor = new TokenBucketExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("default", Algorithm.TOKEN_BUCKET, 10, 0, 2, 3, 0);

        RateLimitResult result = executor.check("user-1", policy, store);

        assertTrue(result.allowed());
        assertEquals(2, result.remaining());
    }

    @Test
    void burstBeyondCapacityIsRejected() {
        AtomicLong clock = new AtomicLong(2_000);
        TokenBucketExecutor executor = new TokenBucketExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("burst", Algorithm.TOKEN_BUCKET, 10, 0, 1, 3, 0);

        executor.check("k", policy, store);
        executor.check("k", policy, store);
        executor.check("k", policy, store);
        RateLimitResult rejected = executor.check("k", policy, store);

        assertFalse(rejected.allowed());
        assertEquals(0, rejected.remaining());
        assertEquals(1, rejected.retryAfter());
    }

    @Test
    void remainingDecrementsOnAllowedRequests() {
        AtomicLong clock = new AtomicLong(3_000);
        TokenBucketExecutor executor = new TokenBucketExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("decrement", Algorithm.TOKEN_BUCKET, 10, 0, 1, 4, 0);

        RateLimitResult first = executor.check("k", policy, store);
        RateLimitResult second = executor.check("k", policy, store);
        RateLimitResult third = executor.check("k", policy, store);

        assertEquals(3, first.remaining());
        assertEquals(2, second.remaining());
        assertEquals(1, third.remaining());
    }

    @Test
    void refillAfterTimeAdvanceAllowsAgain() {
        AtomicLong clock = new AtomicLong(4_000);
        TokenBucketExecutor executor = new TokenBucketExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("refill", Algorithm.TOKEN_BUCKET, 10, 0, 1, 2, 0);

        executor.check("k", policy, store);
        executor.check("k", policy, store);
        RateLimitResult rejected = executor.check("k", policy, store);
        assertFalse(rejected.allowed());

        clock.addAndGet(2);
        RateLimitResult afterRefill = executor.check("k", policy, store);

        assertTrue(afterRefill.allowed());
    }

    @Test
    void retryAfterUsesCeilOneOverRefillRate() {
        AtomicLong clock = new AtomicLong(5_000);
        TokenBucketExecutor executor = new TokenBucketExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("retry", Algorithm.TOKEN_BUCKET, 10, 0, 2, 1, 0);

        executor.check("k", policy, store);
        RateLimitResult rejected = executor.check("k", policy, store);

        assertFalse(rejected.allowed());
        assertEquals(1, rejected.retryAfter());
    }
}
