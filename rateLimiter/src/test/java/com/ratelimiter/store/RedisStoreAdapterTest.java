package com.ratelimiter.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ratelimiter.model.Errors.StoreException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedisStoreAdapterTest {
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisAsyncCommands<String, String> commands;
    private RedisStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        client = mock(RedisClient.class);
        connection = mock(StatefulRedisConnection.class);
        commands = mock(RedisAsyncCommands.class);
        adapter = new RedisStoreAdapter(client, connection, commands);
    }

    @Test
    void getReturnsValueFromRedis() throws Exception {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(commands.get("key")).thenReturn(future);
        when(future.get()).thenReturn("value");

        assertEquals("value", adapter.get("key"));
    }

    @Test
    void evalReturnsMultiResult() throws Exception {
        RedisFuture<Object> future = mock(RedisFuture.class);
        List<Object> expected = List.of(1L, 9L, 1001L);
        when(commands.eval(eq("-- script: token_bucket"), eq(ScriptOutputType.MULTI), any(String[].class), any(String[].class))).thenReturn(future);
        when(future.get()).thenReturn(expected);

        Object result = adapter.eval("-- script: token_bucket", List.of("k"), List.of("10", "1", "1000", "30"));

        assertEquals(expected, result);
    }

    @Test
    void pingReturnsFalseOnFailure() {
        when(commands.ping()).thenThrow(new RuntimeException("down"));

        assertFalse(adapter.ping());
    }

    @Test
    void getWrapsExceptionsAsStoreException() {
        when(commands.get("key")).thenThrow(new RuntimeException("boom"));

        assertThrows(StoreException.class, () -> adapter.get("key"));
    }

    @Test
    void closeClosesConnectionAndClient() {
        adapter.close();

        verify(connection).close();
        verify(client).shutdown(any(Duration.class), any(Duration.class));
    }

    @Test
    void pingReturnsTrueWhenPong() throws Exception {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(commands.ping()).thenReturn(future);
        when(future.get()).thenReturn("PONG");

        assertTrue(adapter.ping());
    }
}
