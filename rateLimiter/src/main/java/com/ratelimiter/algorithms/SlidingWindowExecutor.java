package com.ratelimiter.algorithms;

import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;
import java.util.List;
import java.util.function.LongSupplier;

public class SlidingWindowExecutor implements AlgorithmExecutor {
    private static final String SCRIPT = """
            -- script: sliding_window
            local key         = KEYS[1]
            local now         = tonumber(ARGV[1])
            local windowMs    = tonumber(ARGV[2])
            local limit       = tonumber(ARGV[3])
            local ttlSeconds  = tonumber(ARGV[4])
            local windowStart = now - windowMs

            redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
            local count = redis.call('ZCARD', key)

            if count < limit then
              local member = now .. '-' .. math.random(100000)
              redis.call('ZADD', key, now, member)
              redis.call('EXPIRE', key, ttlSeconds)
              return {1, limit - count - 1, math.ceil((now + windowMs) / 1000)}
            else
              local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
              local retryAfter = math.ceil((tonumber(oldest[2]) + windowMs - now) / 1000)
              return {0, 0, retryAfter}
            end
            """;

    private final LongSupplier nowMs;

    public SlidingWindowExecutor() {
        this(System::currentTimeMillis);
    }

    SlidingWindowExecutor(LongSupplier nowMs) {
        this.nowMs = nowMs;
    }

    @Override
    public RateLimitResult check(String key, RateLimitPolicy policy, StoreAdapter store) {
        long now = nowMs.getAsLong();
        String redisKey = "rl:sw:" + policy.id() + ":" + key;
        int ttlSeconds = (int) Math.ceil(policy.windowMs() / 1000.0d);

        Object raw = store.eval(
                SCRIPT,
                List.of(redisKey),
                List.of(
                        String.valueOf(now),
                        String.valueOf(policy.windowMs()),
                        String.valueOf(policy.limit()),
                        String.valueOf(ttlSeconds)
                )
        );

        List<?> values = (List<?>) raw;
        boolean allowed = number(values.get(0)).intValue() == 1;
        int remaining = number(values.get(1)).intValue();
        long third = number(values.get(2)).longValue();

        if (allowed) {
            return new RateLimitResult(true, policy.limit(), remaining, third, 0);
        }

        int retryAfter = (int) third;
        long resetAt = (now / 1000) + retryAfter;
        return new RateLimitResult(false, policy.limit(), 0, resetAt, retryAfter);
    }

    private static Number number(Object value) {
        if (value instanceof Number n) {
            return n;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
