package com.ratelimiter.grpc;

import com.ratelimiter.grpc.proto.CheckRateLimitRequest;
import com.ratelimiter.grpc.proto.CheckRateLimitResponse;
import com.ratelimiter.grpc.proto.PolicyConfig;
import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResult;
import java.util.List;

public final class ProtoMapper {
    private ProtoMapper() {
    }

    public static RateLimitRequest toRequest(CheckRateLimitRequest proto) {
        return new RateLimitRequest(
                proto.getPolicyId(),
                proto.getKey(),
                proto.getPath(),
                proto.getMethod()
        );
    }

    public static CheckRateLimitResponse toProto(RateLimitResult result) {
        return CheckRateLimitResponse.newBuilder()
                .setAllowed(result.allowed())
                .setLimit(result.limit())
                .setRemaining(result.remaining())
                .setResetAt(result.resetAt())
                .setRetryAfter(result.retryAfter())
                .build();
    }

    public static RateLimitPolicy toPolicy(PolicyConfig proto) {
        return new RateLimitPolicy(
                proto.getPolicyId(),
                toModelAlgorithm(proto.getAlgorithm()),
                proto.getLimit(),
                proto.getWindowMs(),
                proto.getRefillRate(),
                proto.getCapacity(),
                proto.getLeakRate()
        );
    }

    public static List<RateLimitPolicy> toPolicies(List<PolicyConfig> protos) {
        return protos.stream().map(ProtoMapper::toPolicy).toList();
    }

    private static Algorithm toModelAlgorithm(com.ratelimiter.grpc.proto.Algorithm algorithm) {
        return switch (algorithm) {
            case TOKEN_BUCKET -> Algorithm.TOKEN_BUCKET;
            case SLIDING_WINDOW -> Algorithm.SLIDING_WINDOW;
            case LEAKY_BUCKET -> Algorithm.LEAKY_BUCKET;
            case ALGORITHM_UNSPECIFIED -> Algorithm.TOKEN_BUCKET;
            case UNRECOGNIZED -> Algorithm.TOKEN_BUCKET;
        };
    }
}
