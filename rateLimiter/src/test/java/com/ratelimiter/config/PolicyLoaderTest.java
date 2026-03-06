package com.ratelimiter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.Errors.ConfigValidationException;
import com.ratelimiter.model.RateLimitPolicy;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PolicyLoaderTest {

    @Test
    void defaultYamlLoadsThreePolicies() {
        PolicyLoader loader = new PolicyLoader();

        RateLimitPolicy defaultPolicy = loader.getPolicy("default");
        RateLimitPolicy strictPolicy = loader.getPolicy("strict");
        RateLimitPolicy meteredPolicy = loader.getPolicy("metered");

        assertEquals(Algorithm.TOKEN_BUCKET, defaultPolicy.algorithm());
        assertEquals(Algorithm.SLIDING_WINDOW, strictPolicy.algorithm());
        assertEquals(Algorithm.LEAKY_BUCKET, meteredPolicy.algorithm());
    }

    @Test
    void missingRequiredFieldsCollectAllErrors() {
        String yaml = """
                policies:
                  - id: bad
                    algorithm: TOKEN_BUCKET
                    limit: 0
                  - id: missing_alg
                    limit: 10
                """;

        ConfigValidationException ex = assertThrows(
                ConfigValidationException.class,
                () -> PolicyLoader.parsePolicies(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
        );

        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("policy[bad]: limit must be > 0")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("policy[bad]: refillRate must be > 0")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("policy[bad]: capacity must be > 0")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("policy[missing_alg]: algorithm is required")));
    }

    @Test
    void duplicatePolicyIdThrowsConfigValidation() {
        List<RateLimitPolicy> duplicate = List.of(
                new RateLimitPolicy("dup", Algorithm.TOKEN_BUCKET, 10, 0, 1, 10, 0),
                new RateLimitPolicy("dup", Algorithm.SLIDING_WINDOW, 10, 1_000, 0, 0, 0)
        );

        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> new PolicyLoader(duplicate));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("duplicate policy id")));
    }

    @Test
    void reloadInvalidPoliciesKeepsOriginalState() {
        PolicyLoader loader = new PolicyLoader(List.of(
                new RateLimitPolicy("good", Algorithm.TOKEN_BUCKET, 100, 0, 10, 100, 0)
        ));

        assertThrows(
                ConfigValidationException.class,
                () -> loader.reload(List.of(new RateLimitPolicy("bad", Algorithm.TOKEN_BUCKET, 0, 0, 10, 100, 0)))
        );

        assertEquals(100, loader.getPolicy("good").limit());
    }

    @Test
    void tokenBucketLimitMustMatchCapacity() {
        ConfigValidationException ex = assertThrows(
                ConfigValidationException.class,
                () -> new PolicyLoader(List.of(
                        new RateLimitPolicy("bad", Algorithm.TOKEN_BUCKET, 50, 0, 10, 100, 0)
                ))
        );
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("limit must equal capacity for TOKEN_BUCKET")));
    }

    @Test
    void concurrentReadsDuringReloadNeverSeeNullOrPartialMap() throws Exception {
        PolicyLoader loader = new PolicyLoader(List.of(
                new RateLimitPolicy("p1", Algorithm.TOKEN_BUCKET, 10, 0, 1, 10, 0),
                new RateLimitPolicy("p2", Algorithm.TOKEN_BUCKET, 10, 0, 1, 10, 0)
        ));

        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(8);

        Future<?> reader = pool.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < 5_000; i++) {
                    assertNotNull(loader.getPolicy("p1"));
                    assertNotNull(loader.getPolicy("p2"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        Future<?> reloader = pool.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < 250; i++) {
                    loader.reload(List.of(
                            new RateLimitPolicy("p1", Algorithm.TOKEN_BUCKET, 10 + i, 0, 1, 10 + i, 0),
                            new RateLimitPolicy("p2", Algorithm.TOKEN_BUCKET, 20 + i, 0, 1, 20 + i, 0)
                    ));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        start.countDown();
        reader.get(10, TimeUnit.SECONDS);
        reloader.get(10, TimeUnit.SECONDS);

        pool.shutdownNow();
    }
}
