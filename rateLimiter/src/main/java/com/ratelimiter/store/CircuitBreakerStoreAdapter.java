package com.ratelimiter.store;

import com.ratelimiter.model.Errors.StoreException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public class CircuitBreakerStoreAdapter implements StoreAdapter {
    private final StoreAdapter delegate;
    private final int failureThreshold;
    private final int successThreshold;
    private final long cooldownMs;
    private final LongSupplier clock;

    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong openedAtMs = new AtomicLong(0);

    public CircuitBreakerStoreAdapter(StoreAdapter delegate) {
        this(delegate, 5, 2, 10_000, System::currentTimeMillis);
    }

    public CircuitBreakerStoreAdapter(
            StoreAdapter delegate,
            int failureThreshold,
            int successThreshold,
            long cooldownMs,
            LongSupplier clock
    ) {
        this.delegate = delegate;
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.cooldownMs = cooldownMs;
        this.clock = clock;
    }

    @Override
    public String get(String key) throws StoreException {
        ensureCallAllowed();
        try {
            String result = delegate.get(key);
            onSuccess();
            return result;
        } catch (StoreException e) {
            onFailure();
            throw e;
        }
    }

    @Override
    public void set(String key, String value, long ttlMs) throws StoreException {
        ensureCallAllowed();
        try {
            delegate.set(key, value, ttlMs);
            onSuccess();
        } catch (StoreException e) {
            onFailure();
            throw e;
        }
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) throws StoreException {
        ensureCallAllowed();
        try {
            Object result = delegate.eval(script, keys, args);
            onSuccess();
            return result;
        } catch (StoreException e) {
            onFailure();
            throw e;
        }
    }

    @Override
    public boolean ping() {
        return delegate.ping();
    }

    @Override
    public void close() {
        delegate.close();
    }

    public CircuitState getState() {
        return state.get();
    }

    public void reset() {
        state.set(CircuitState.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        openedAtMs.set(0);
    }

    private void ensureCallAllowed() {
        CircuitState current = state.get();
        if (current == CircuitState.OPEN) {
            long now = clock.getAsLong();
            if (now - openedAtMs.get() >= cooldownMs) {
                if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    successCount.set(0);
                }
            } else {
                throw new StoreException("Circuit open — store unavailable");
            }
        }
    }

    private void onSuccess() {
        CircuitState current = state.get();
        if (current == CircuitState.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            if (successes >= successThreshold) {
                state.set(CircuitState.CLOSED);
                failureCount.set(0);
                successCount.set(0);
            }
            return;
        }

        if (current == CircuitState.CLOSED) {
            failureCount.set(0);
        }
    }

    private void onFailure() {
        CircuitState current = state.get();
        if (current == CircuitState.HALF_OPEN) {
            tripOpen();
            return;
        }

        if (current == CircuitState.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                tripOpen();
            }
        }
    }

    private void tripOpen() {
        state.set(CircuitState.OPEN);
        openedAtMs.set(clock.getAsLong());
        failureCount.set(0);
        successCount.set(0);
    }
}
