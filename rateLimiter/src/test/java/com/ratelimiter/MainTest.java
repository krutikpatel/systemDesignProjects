package com.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.grpc.DeadlineInterceptor;
import com.ratelimiter.grpc.LoggingInterceptor;
import com.ratelimiter.model.FailMode;
import com.ratelimiter.store.CircuitBreakerStoreAdapter;
import com.ratelimiter.store.InMemoryStoreAdapter;
import com.ratelimiter.store.StoreAdapter;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void runtimeConfigUsesDefaultsWhenMissing() {
        Main.RuntimeConfig config = Main.RuntimeConfig.fromEnv(Map.of());
        assertEquals("redis://localhost:6379", config.redisUri());
        assertEquals(50051, config.grpcPort());
        assertEquals(9090, config.metricsPort());
        assertEquals(FailMode.OPEN, config.failMode());
    }

    @Test
    void runtimeConfigRejectsInvalidPort() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Main.RuntimeConfig.fromEnv(Map.of("GRPC_PORT", "abc"))
        );
        assertTrue(ex.getMessage().contains("GRPC_PORT"));
    }

    @Test
    void runtimeConfigRejectsInvalidFailMode() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Main.RuntimeConfig.fromEnv(Map.of("FAIL_MODE", "BROKEN"))
        );
        assertTrue(ex.getMessage().contains("FAIL_MODE"));
    }

    @Test
    void wrapWithCircuitBreakerWrapsDelegateStore() {
        StoreAdapter store = Main.wrapWithCircuitBreaker(new InMemoryStoreAdapter());
        assertTrue(store instanceof CircuitBreakerStoreAdapter);
        store.close();
    }

    @Test
    void interceptorsIncludeDeadlineThenLogging() {
        var interceptors = Main.defaultInterceptors();
        assertEquals(2, interceptors.length);
        assertTrue(interceptors[0] instanceof DeadlineInterceptor);
        assertTrue(interceptors[1] instanceof LoggingInterceptor);
    }
}
