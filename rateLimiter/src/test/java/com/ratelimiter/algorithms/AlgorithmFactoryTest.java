package com.ratelimiter.algorithms;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.ratelimiter.model.Algorithm;
import org.junit.jupiter.api.Test;

class AlgorithmFactoryTest {

    @Test
    void createReturnsTokenBucketExecutor() {
        assertInstanceOf(TokenBucketExecutor.class, AlgorithmFactory.create(Algorithm.TOKEN_BUCKET));
    }

    @Test
    void createReturnsSlidingWindowExecutor() {
        assertInstanceOf(SlidingWindowExecutor.class, AlgorithmFactory.create(Algorithm.SLIDING_WINDOW));
    }

    @Test
    void createReturnsLeakyBucketExecutor() {
        assertInstanceOf(LeakyBucketExecutor.class, AlgorithmFactory.create(Algorithm.LEAKY_BUCKET));
    }
}
