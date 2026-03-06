package com.ratelimiter.algorithms;

import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;
import java.time.Instant;
import java.util.List;
import java.util.function.LongSupplier;

public class TokenBucketExecutor implements AlgorithmExecutor {
    private static final String SCRIPT = """
            -- script: token_bucket
            local key        = KEYS[1]
            local capacity   = tonumber(ARGV[1])
            local refillRate = tonumber(ARGV[2])
            local now        = tonumber(ARGV[3])
            local ttl        = tonumber(ARGV[4])

            local data       = redis.call('HMGET', key, 'tokens', 'lastRefill')
            local tokens     = tonumber(data[1]) or capacity
            local lastRefill = tonumber(data[2]) or now

            local elapsed    = math.max(0, now - lastRefill)
            local newTokens  = math.min(capacity, tokens + elapsed * refillRate)

            if newTokens >= 1 then
              redis.call('HMSET', key, 'tokens', newTokens - 1, 'lastRefill', now)
              redis.call('EXPIRE', key, ttl)
              return {1, math.floor(newTokens - 1), now + math.ceil(1 / refillRate)}
            else
              return {0, 0, now + math.ceil(1 / refillRate)}
            end
            """;

    private final LongSupplier nowSeconds;
    private final String keyPrefix;

    public TokenBucketExecutor() {
        this(() -> Instant.now().getEpochSecond(), "rl:tb");
    }

    TokenBucketExecutor(LongSupplier nowSeconds) {
        this(nowSeconds, "rl:tb");
    }

    TokenBucketExecutor(LongSupplier nowSeconds, String keyPrefix) {
        this.nowSeconds = nowSeconds;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy, StoreAdapter store) {
        String redisKey = keyPrefix + ":" + policy.id() + ":" + key;
        long now = nowSeconds.getAsLong();

        int ttlSeconds = Math.max(1, (int) Math.ceil((double) policy.capacity() / policy.refillRate()) * 2);

        Object raw = store.eval(
                SCRIPT,
                List.of(redisKey),
                List.of(
                        String.valueOf(policy.capacity()),
                        String.valueOf(policy.refillRate()),
                        String.valueOf(now),
                        String.valueOf(ttlSeconds)
                )
        );

        List<?> values = (List<?>) raw;
        boolean allowed = number(values.get(0)).intValue() == 1;
        int remaining = number(values.get(1)).intValue();
        long resetAt = number(values.get(2)).longValue();
        int retryAfter = allowed ? 0 : (int) Math.ceil(1.0d / policy.refillRate());

        return new RateLimitResult(allowed, policy.limit(), remaining, resetAt, retryAfter);
    }

    private static Number number(Object value) {
        if (value instanceof Number n) {
            return n;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
