package com.ratelimiter.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Errors.StoreException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryStoreAdapterTest {

    @Test
    void setAndGetRespectsTtl() throws Exception {
        InMemoryStoreAdapter adapter = new InMemoryStoreAdapter();

        adapter.set("key", "value", 5);
        assertEquals("value", adapter.get("key"));

        Thread.sleep(20);
        assertNull(adapter.get("key"));
    }

    @Test
    void pingAlwaysTrue() {
        InMemoryStoreAdapter adapter = new InMemoryStoreAdapter();

        assertTrue(adapter.ping());
    }

    @Test
    void tokenBucketScriptDispatchWorks() {
        InMemoryStoreAdapter adapter = new InMemoryStoreAdapter();

        Object result = adapter.eval(
                "-- script: token_bucket\nlocal x = 1",
                List.of("bucket:key"),
                List.of("2", "1", "1000", "10")
        );

        assertTrue(result instanceof List<?>);
        assertEquals(1L, ((List<?>) result).getFirst());
    }

    @Test
    void slidingWindowScriptDispatchWorks() {
        InMemoryStoreAdapter adapter = new InMemoryStoreAdapter();

        Object result = adapter.eval(
                "-- script: sliding_window",
                List.of("window:key"),
                List.of("1000", "60000", "3", "60")
        );

        assertTrue(result instanceof List<?>);
        assertEquals(1L, ((List<?>) result).getFirst());
    }

    @Test
    void unknownScriptThrowsStoreException() {
        InMemoryStoreAdapter adapter = new InMemoryStoreAdapter();

        assertThrows(
                StoreException.class,
                () -> adapter.eval("-- script: unknown", List.of("k"), List.of("1", "2", "3", "4"))
        );
    }

    @Test
    void slidingWindowEvalIsThreadSafeUnderConcurrency() throws Exception {
        InMemoryStoreAdapter adapter = new InMemoryStoreAdapter();
        int workers = 40;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger failures = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < workers; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        adapter.eval(
                                "-- script: sliding_window",
                                List.of("window:key"),
                                List.of(String.valueOf(System.currentTimeMillis()), "60000", "10", "60")
                        );
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
        }

        assertEquals(0, failures.get());
    }
}
