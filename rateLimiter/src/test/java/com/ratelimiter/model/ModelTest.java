package com.ratelimiter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.ratelimiter.model.Errors.ConfigValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Test
    void rateLimitPolicyEqualityAndHashCodeWork() {
        RateLimitPolicy first = new RateLimitPolicy(
                "default",
                Algorithm.TOKEN_BUCKET,
                100,
                60_000,
                10,
                100,
                10
        );
        RateLimitPolicy second = new RateLimitPolicy(
                "default",
                Algorithm.TOKEN_BUCKET,
                100,
                60_000,
                10,
                100,
                10
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.toString(), second.toString());
    }

    @Test
    void rateLimitPolicyCopyWithModificationWorks() {
        RateLimitPolicy original = new RateLimitPolicy(
                "default",
                Algorithm.TOKEN_BUCKET,
                100,
                60_000,
                10,
                100,
                10
        );

        RateLimitPolicy modified = new RateLimitPolicy(
                original.id(),
                original.algorithm(),
                200,
                original.windowMs(),
                original.refillRate(),
                original.capacity(),
                original.leakRate()
        );

        assertNotEquals(original, modified);
        assertEquals(100, original.limit());
        assertEquals(200, modified.limit());
    }

    @Test
    void configValidationExceptionExposesErrors() {
        List<String> expectedErrors = List.of("limit must be > 0", "id must be non-empty");

        ConfigValidationException exception = new ConfigValidationException(expectedErrors);

        assertEquals(expectedErrors, exception.getErrors());
    }
}
