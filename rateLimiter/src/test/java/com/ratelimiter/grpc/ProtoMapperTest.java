package com.ratelimiter.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ratelimiter.grpc.proto.Algorithm;
import com.ratelimiter.grpc.proto.CheckRateLimitRequest;
import com.ratelimiter.grpc.proto.PolicyConfig;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.Test;

class ProtoMapperTest {

    @Test
    void toRequestMapsAllFields() {
        CheckRateLimitRequest proto = CheckRateLimitRequest.newBuilder()
                .setPolicyId("p1")
                .setKey("k1")
                .setPath("/v1")
                .setMethod("GET")
                .build();

        RateLimitRequest request = ProtoMapper.toRequest(proto);

        assertEquals("p1", request.policyId());
        assertEquals("k1", request.key());
        assertEquals("/v1", request.path());
        assertEquals("GET", request.method());
    }

    @Test
    void toProtoMapsAllFields() {
        RateLimitResult result = new RateLimitResult(true, 100, 99, 1234L, 0);

        var proto = ProtoMapper.toProto(result);

        assertEquals(true, proto.getAllowed());
        assertEquals(100, proto.getLimit());
        assertEquals(99, proto.getRemaining());
        assertEquals(1234L, proto.getResetAt());
        assertEquals(0, proto.getRetryAfter());
    }

    @Test
    void toPolicyMapsPolicyConfig() {
        PolicyConfig config = PolicyConfig.newBuilder()
                .setPolicyId("strict")
                .setAlgorithm(Algorithm.SLIDING_WINDOW)
                .setLimit(10)
                .setWindowMs(10_000)
                .setRefillRate(0)
                .setCapacity(0)
                .setLeakRate(0)
                .build();

        RateLimitPolicy policy = ProtoMapper.toPolicy(config);

        assertEquals("strict", policy.id());
        assertEquals(com.ratelimiter.model.Algorithm.SLIDING_WINDOW, policy.algorithm());
        assertEquals(10, policy.limit());
        assertEquals(10_000, policy.windowMs());
    }
}
