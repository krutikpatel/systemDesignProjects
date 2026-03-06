package com.ratelimiter.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import com.ratelimiter.model.Errors.StoreException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CircuitBreakerStoreAdapterTest {

    @Test
    void opensAfterFailureThreshold() {
        StoreAdapter delegate = mock(StoreAdapter.class);
        AtomicLong clock = new AtomicLong(0);
        CircuitBreakerStoreAdapter breaker = new CircuitBreakerStoreAdapter(delegate, 5, 2, 10_000, clock::get);
        when(delegate.get(anyString())).thenThrow(new StoreException("down"));

        for (int i = 0; i < 5; i++) {
            assertThrows(StoreException.class, () -> breaker.get("k"));
        }

        assertEquals(CircuitState.OPEN, breaker.getState());
    }

    @Test
    void openStateSkipsDelegateCalls() {
        StoreAdapter delegate = mock(StoreAdapter.class);
        AtomicLong clock = new AtomicLong(0);
        CircuitBreakerStoreAdapter breaker = new CircuitBreakerStoreAdapter(delegate, 1, 2, 10_000, clock::get);
        when(delegate.get(anyString())).thenThrow(new StoreException("down"));

        assertThrows(StoreException.class, () -> breaker.get("k")); // trips open
        assertThrows(StoreException.class, () -> breaker.get("k")); // short-circuit

        verify(delegate, times(1)).get("k");
    }

    @Test
    void transitionsToHalfOpenAfterCooldown() {
        StoreAdapter delegate = mock(StoreAdapter.class);
        AtomicLong clock = new AtomicLong(0);
        CircuitBreakerStoreAdapter breaker = new CircuitBreakerStoreAdapter(delegate, 1, 2, 10, clock::get);
        when(delegate.get(anyString())).thenThrow(new StoreException("down"));

        assertThrows(StoreException.class, () -> breaker.get("k"));
        assertEquals(CircuitState.OPEN, breaker.getState());

        clock.set(11);
        doReturn("ok").when(delegate).get(anyString());
        breaker.get("k");

        assertEquals(CircuitState.HALF_OPEN, breaker.getState());
    }

    @Test
    void halfOpenClosesAfterSuccessThreshold() {
        StoreAdapter delegate = mock(StoreAdapter.class);
        AtomicLong clock = new AtomicLong(0);
        CircuitBreakerStoreAdapter breaker = new CircuitBreakerStoreAdapter(delegate, 1, 2, 10, clock::get);
        when(delegate.get(anyString())).thenThrow(new StoreException("down"));

        assertThrows(StoreException.class, () -> breaker.get("k"));
        clock.set(11);

        doReturn("ok").when(delegate).get(anyString());
        breaker.get("k");
        breaker.get("k");

        assertEquals(CircuitState.CLOSED, breaker.getState());
    }

    @Test
    void halfOpenFailureReopensAndResetsCooldown() {
        StoreAdapter delegate = mock(StoreAdapter.class);
        AtomicLong clock = new AtomicLong(0);
        CircuitBreakerStoreAdapter breaker = new CircuitBreakerStoreAdapter(delegate, 1, 2, 10, clock::get);
        when(delegate.get(anyString())).thenThrow(new StoreException("down"));

        assertThrows(StoreException.class, () -> breaker.get("k"));
        clock.set(11);

        assertThrows(StoreException.class, () -> breaker.get("k"));
        assertEquals(CircuitState.OPEN, breaker.getState());

        clock.set(15); // still within renewed cooldown
        assertThrows(StoreException.class, () -> breaker.get("k"));
        verify(delegate, times(2)).get("k");
    }
}
