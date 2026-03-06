package com.ratelimiter.algorithms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.InMemoryStoreAdapter;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class SlidingWindowExecutorTest {

    @Test
    void exactlyLimitRequestsSucceedThenReject() {
        AtomicLong clock = new AtomicLong(1_000);
        SlidingWindowExecutor executor = new SlidingWindowExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("sw", Algorithm.SLIDING_WINDOW, 3, 1_000, 0, 0, 0);

        assertTrue(executor.check("k", policy, store).allowed());
        assertTrue(executor.check("k", policy, store).allowed());
        assertTrue(executor.check("k", policy, store).allowed());
        assertFalse(executor.check("k", policy, store).allowed());
    }

    @Test
    void retryAfterIsPositiveWhenRejected() {
        AtomicLong clock = new AtomicLong(2_000);
        SlidingWindowExecutor executor = new SlidingWindowExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("sw", Algorithm.SLIDING_WINDOW, 1, 1_000, 0, 0, 0);

        executor.check("k", policy, store);
        RateLimitResult denied = executor.check("k", policy, store);

        assertFalse(denied.allowed());
        assertTrue(denied.retryAfter() > 0);
    }

    @Test
    void windowSlideOpensExactlyOneSlot() {
        AtomicLong clock = new AtomicLong(0);
        SlidingWindowExecutor executor = new SlidingWindowExecutor(clock::get);
        InMemoryStoreAdapter store = new InMemoryStoreAdapter();
        RateLimitPolicy policy = new RateLimitPolicy("sw", Algorithm.SLIDING_WINDOW, 2, 1_000, 0, 0, 0);

        assertTrue(executor.check("k", policy, store).allowed()); // t=0
        clock.set(500);
        assertTrue(executor.check("k", policy, store).allowed()); // t=500
        assertFalse(executor.check("k", policy, store).allowed()); // full

        clock.set(1_001); // t=0 request expires, t=500 remains
        assertTrue(executor.check("k", policy, store).allowed()); // one slot reopened
        assertFalse(executor.check("k", policy, store).allowed()); // full again
    }
}
