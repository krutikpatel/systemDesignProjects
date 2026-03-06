package com.ratelimiter.grpc;

import com.ratelimiter.grpc.proto.CheckRateLimitRequest;
import com.ratelimiter.grpc.proto.CheckRateLimitResponse;
import com.ratelimiter.grpc.proto.PolicyConfig;
import com.ratelimiter.model.Errors.ConfigValidationException;
import com.ratelimiter.model.Algorithm;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitRequest;
import com.ratelimiter.model.RateLimitResult;
import java.util.ArrayList;
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
        List<RateLimitPolicy> mapped = new ArrayList<>(protos.size());
        List<String> errors = new ArrayList<>();
        for (PolicyConfig proto : protos) {
            try {
                mapped.add(toPolicy(proto));
            } catch (IllegalArgumentException ex) {
                String policyId = proto.getPolicyId().isBlank() ? "<missing-policy-id>" : proto.getPolicyId();
                errors.add("policy[" + policyId + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
        return mapped;
    }

    private static Algorithm toModelAlgorithm(com.ratelimiter.grpc.proto.Algorithm algorithm) {
        return switch (algorithm) {
            case TOKEN_BUCKET -> Algorithm.TOKEN_BUCKET;
            case SLIDING_WINDOW -> Algorithm.SLIDING_WINDOW;
            case LEAKY_BUCKET -> Algorithm.LEAKY_BUCKET;
            case ALGORITHM_UNSPECIFIED -> throw new IllegalArgumentException("algorithm must be specified");
            case UNRECOGNIZED -> throw new IllegalArgumentException("algorithm is unrecognized");
        };
    }
}
