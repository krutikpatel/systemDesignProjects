package com.ratelimiter.store;

import com.ratelimiter.model.Errors.StoreException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStoreAdapter implements StoreAdapter {
    private final Map<String, Entry> kvStore = new ConcurrentHashMap<>();
    private final Map<String, TokenBucketState> tokenBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> slidingWindows = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        Entry entry = kvStore.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.expiresAtMs < System.currentTimeMillis()) {
            kvStore.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public void set(String key, String value, long ttlMs) {
        long expiresAt = System.currentTimeMillis() + Math.max(0, ttlMs);
        kvStore.put(key, new Entry(value, expiresAt));
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        String scriptName = extractScriptName(script);
        return switch (scriptName) {
            case "token_bucket" -> executeTokenBucket(keys, args);
            case "sliding_window" -> executeSlidingWindow(keys, args);
            case "leaky_bucket" -> executeLeakyBucket(keys, args);
            default -> throw new StoreException("Unsupported in-memory script: " + scriptName);
        };
    }

    @Override
    public boolean ping() {
        return true;
    }

    @Override
    public void close() {
        kvStore.clear();
        tokenBuckets.clear();
        slidingWindows.clear();
    }

    private static String extractScriptName(String script) {
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-- script:")) {
                return trimmed.substring("-- script:".length()).trim();
            }
        }
        throw new StoreException("Script header missing: -- script: <name>");
    }

    private List<Object> executeTokenBucket(List<String> keys, List<String> args) {
        requireSizes(keys, args, 1, 4);
        String key = keys.getFirst();

        int capacity = Integer.parseInt(args.get(0));
        int refillRate = Integer.parseInt(args.get(1));
        long now = Long.parseLong(args.get(2));

        TokenBucketState state = tokenBuckets.computeIfAbsent(
                key,
                k -> new TokenBucketState(capacity, now)
        );

        synchronized (state) {
            long elapsed = Math.max(0, now - state.lastRefillSeconds);
            double refreshed = Math.min(capacity, state.tokens + elapsed * refillRate);

            if (refreshed >= 1.0d) {
                state.tokens = refreshed - 1.0d;
                state.lastRefillSeconds = now;
                return List.of(
                        1L,
                        Math.max(0L, (long) Math.floor(state.tokens)),
                        now + Math.max(1L, ceilDiv(1, refillRate))
                );
            }
        }
        return List.of(0L, 0L, now + Math.max(1L, ceilDiv(1, refillRate)));
    }

    private List<Object> executeLeakyBucket(List<String> keys, List<String> args) {
        return executeTokenBucket(keys, args);
    }

    private List<Object> executeSlidingWindow(List<String> keys, List<String> args) {
        requireSizes(keys, args, 1, 4);
        String key = keys.getFirst();

        long nowMs = Long.parseLong(args.get(0));
        long windowMs = Long.parseLong(args.get(1));
        int limit = Integer.parseInt(args.get(2));

        Deque<Long> timestamps = slidingWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            long windowStart = nowMs - windowMs;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.removeFirst();
            }

            if (timestamps.size() < limit) {
                timestamps.addLast(nowMs);
                long remaining = limit - timestamps.size();
                long resetAt = (nowMs + windowMs + 999) / 1000;
                return List.of(1L, Math.max(0L, remaining), resetAt);
            }

            long oldest = Objects.requireNonNullElse(timestamps.peekFirst(), nowMs);
            long retryAfter = Math.max(1, (oldest + windowMs - nowMs + 999) / 1000);
            return List.of(0L, 0L, retryAfter);
        }
    }

    private static void requireSizes(List<String> keys, List<String> args, int expectedKeys, int expectedArgs) {
        if (keys.size() < expectedKeys || args.size() < expectedArgs) {
            throw new StoreException("Invalid key/arg count for script evaluation");
        }
    }

    private static long ceilDiv(long numerator, long denominator) {
        if (denominator <= 0) {
            return 1;
        }
        return (numerator + denominator - 1) / denominator;
    }

    private record Entry(String value, long expiresAtMs) {
    }

    private static final class TokenBucketState {
        private double tokens;
        private long lastRefillSeconds;

        private TokenBucketState(double tokens, long lastRefillSeconds) {
            this.tokens = tokens;
            this.lastRefillSeconds = lastRefillSeconds;
        }
    }
}
