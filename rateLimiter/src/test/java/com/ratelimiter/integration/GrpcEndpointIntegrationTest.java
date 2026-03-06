package com.ratelimiter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.grpc.proto.Algorithm;
import com.ratelimiter.grpc.proto.CheckRateLimitRequest;
import com.ratelimiter.grpc.proto.PolicyConfig;
import com.ratelimiter.grpc.proto.ReloadConfigRequest;
import org.junit.jupiter.api.Test;

class GrpcEndpointIntegrationTest extends IntegrationTestBase {

    @Test
    void grpcEndpointsWorkEndToEnd() {
        var health = grpcStub.healthCheck(com.ratelimiter.grpc.proto.HealthCheckRequest.newBuilder().build());
        assertTrue(health.getStoreOk());

        PolicyConfig policy = PolicyConfig.newBuilder()
                .setPolicyId("default")
                .setAlgorithm(Algorithm.TOKEN_BUCKET)
                .setLimit(1)
                .setCapacity(1)
                .setRefillRate(1)
                .build();

        var reloadResponse = grpcStub.reloadConfig(
                ReloadConfigRequest.newBuilder().addPolicies(policy).build()
        );
        assertTrue(reloadResponse.getSuccess());
        assertEquals(1, reloadResponse.getPolicyCount());

        var first = grpcStub.checkRateLimit(CheckRateLimitRequest.newBuilder()
                .setPolicyId("default")
                .setKey("grpc-key")
                .setPath("/")
                .setMethod("GET")
                .build());
        var second = grpcStub.checkRateLimit(CheckRateLimitRequest.newBuilder()
                .setPolicyId("default")
                .setKey("grpc-key")
                .setPath("/")
                .setMethod("GET")
                .build());

        assertTrue(first.getAllowed());
        assertFalse(second.getAllowed());
    }
}
