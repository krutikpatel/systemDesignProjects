package com.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.Errors.RateLimiterUnavailableException;
import com.ratelimiter.model.Errors.StoreException;
import com.ratelimiter.model.FailMode;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

    @Test
    void checkReturnsAllowedResult() {
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        RateLimitPolicy policy = new RateLimitPolicy("default", Algorithm.TOKEN_BUCKET, 10, 0, 1, 2, 0);
        when(loader.getPolicy("default")).thenReturn(policy);
        when(store.eval(anyString(), anyList(), anyList())).thenReturn(List.of(1L, 1L, 1001L));

        RateLimiterService service = new RateLimiterService(loader, store, FailMode.CLOSED, null);
        RateLimitResult result = service.check(new RateLimitRequest("default", "k", "/", "GET"));

        assertTrue(result.allowed());
    }

    @Test
    void onThrottleInvokedOncePerRejectedRequest() {
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        RateLimitPolicy policy = new RateLimitPolicy("default", Algorithm.TOKEN_BUCKET, 10, 0, 1, 1, 0);
        when(loader.getPolicy("default")).thenReturn(policy);
        when(store.eval(anyString(), anyList(), anyList())).thenReturn(List.of(0L, 0L, 1001L));

        AtomicInteger throttleCalls = new AtomicInteger();
        RateLimiterService service = new RateLimiterService(
                loader,
                store,
                FailMode.CLOSED,
                (req, res) -> throttleCalls.incrementAndGet()
        );

        RateLimitResult result = service.check(new RateLimitRequest("default", "k", "/", "GET"));

        assertEquals(1, throttleCalls.get());
        assertFalse(result.allowed());
    }

    @Test
    void storeExceptionInOpenModeAllowsRequestWithDegradedSignal() {
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        RateLimitPolicy policy = new RateLimitPolicy("default", Algorithm.TOKEN_BUCKET, 10, 0, 1, 1, 0);
        when(loader.getPolicy("default")).thenReturn(policy);
        when(store.eval(anyString(), anyList(), anyList())).thenThrow(new StoreException("down"));

        RateLimiterService service = new RateLimiterService(loader, store, FailMode.OPEN, null);
        RateLimitResult result = service.check(new RateLimitRequest("default", "k", "/", "GET"));

        assertTrue(result.allowed());
        assertEquals(-1, result.remaining());
    }

    @Test
    void storeExceptionInClosedModeThrowsUnavailable() {
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        RateLimitPolicy policy = new RateLimitPolicy("default", Algorithm.TOKEN_BUCKET, 10, 0, 1, 1, 0);
        when(loader.getPolicy("default")).thenReturn(policy);
        when(store.eval(anyString(), anyList(), anyList())).thenThrow(new StoreException("down"));

        RateLimiterService service = new RateLimiterService(loader, store, FailMode.CLOSED, null);

        assertThrows(
                RateLimiterUnavailableException.class,
                () -> service.check(new RateLimitRequest("default", "k", "/", "GET"))
        );
    }
}
