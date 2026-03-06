package com.ratelimiter.store;

import com.ratelimiter.metrics.MetricsRegistry;
import com.ratelimiter.model.Errors.StoreException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.time.Duration;
import java.util.List;

public class RedisStoreAdapter implements StoreAdapter {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisAsyncCommands<String, String> commands;
    private final MetricsRegistry metricsRegistry;

    public RedisStoreAdapter(String redisUri) {
        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.commands = connection.async();
        this.metricsRegistry = null;
    }

    public RedisStoreAdapter(String redisUri, MetricsRegistry metricsRegistry) {
        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.commands = connection.async();
        this.metricsRegistry = metricsRegistry;
    }

    RedisStoreAdapter(
            RedisClient client,
            StatefulRedisConnection<String, String> connection,
            RedisAsyncCommands<String, String> commands
    ) {
        this(client, connection, commands, null);
    }

    RedisStoreAdapter(
            RedisClient client,
            StatefulRedisConnection<String, String> connection,
            RedisAsyncCommands<String, String> commands,
            MetricsRegistry metricsRegistry
    ) {
        this.client = client;
        this.connection = connection;
        this.commands = commands;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public String get(String key) throws StoreException {
        long start = System.nanoTime();
        try {
            String value = commands.get(key).get();
            recordStoreOp("get", start);
            return value;
        } catch (Exception e) {
            recordStoreError("get");
            throw new StoreException("Failed to GET key: " + key, e);
        }
    }

    @Override
    public void set(String key, String value, long ttlMs) throws StoreException {
        long start = System.nanoTime();
        try {
            commands.psetex(key, Math.max(1, ttlMs), value).get();
            recordStoreOp("set", start);
        } catch (Exception e) {
            recordStoreError("set");
            throw new StoreException("Failed to SET key: " + key, e);
        }
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) throws StoreException {
        long start = System.nanoTime();
        try {
            String[] keysArray = keys.toArray(String[]::new);
            String[] argsArray = args.toArray(String[]::new);
            Object value = commands.eval(script, ScriptOutputType.MULTI, keysArray, argsArray).get();
            recordStoreOp("eval", start);
            return value;
        } catch (Exception e) {
            recordStoreError("eval");
            throw new StoreException("Failed to EVAL script", e);
        }
    }

    @Override
    public boolean ping() {
        try {
            String result = commands.ping().get();
            return "PONG".equalsIgnoreCase(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } finally {
            client.shutdown(Duration.ofSeconds(1), Duration.ofSeconds(1));
        }
    }

    private void recordStoreOp(String operation, long startNanos) {
        if (metricsRegistry == null) {
            return;
        }
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        metricsRegistry.recordStoreOp(operation, durationMs);
    }

    private void recordStoreError(String operation) {
        if (metricsRegistry != null) {
            metricsRegistry.recordStoreError(operation);
        }
    }
}
