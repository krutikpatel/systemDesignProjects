package com.ratelimiter.grpc;

import static net.logstash.logback.marker.Markers.append;

import com.ratelimiter.RateLimiterService;
import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.grpc.proto.CheckRateLimitRequest;
import com.ratelimiter.grpc.proto.CheckRateLimitResponse;
import com.ratelimiter.grpc.proto.HealthCheckRequest;
import com.ratelimiter.grpc.proto.HealthCheckResponse;
import com.ratelimiter.grpc.proto.RateLimiterServiceGrpc;
import com.ratelimiter.grpc.proto.ReloadConfigRequest;
import com.ratelimiter.grpc.proto.ReloadConfigResponse;
import com.ratelimiter.model.Errors.ConfigValidationException;
import com.ratelimiter.model.Errors.PolicyNotFoundException;
import com.ratelimiter.model.Errors.RateLimiterUnavailableException;
import com.ratelimiter.store.StoreAdapter;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimiterGrpcHandler extends RateLimiterServiceGrpc.RateLimiterServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(RateLimiterGrpcHandler.class);

    private final RateLimiterService rateLimiterService;
    private final PolicyLoader policyLoader;
    private final StoreAdapter store;

    public RateLimiterGrpcHandler(
            RateLimiterService rateLimiterService,
            PolicyLoader policyLoader,
            StoreAdapter store
    ) {
        this.rateLimiterService = rateLimiterService;
        this.policyLoader = policyLoader;
        this.store = store;
    }

    @Override
    public void checkRateLimit(CheckRateLimitRequest request, StreamObserver<CheckRateLimitResponse> observer) {
        long startNanos = System.nanoTime();
        try {
            var result = rateLimiterService.check(ProtoMapper.toRequest(request));
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

            var marker = append("policyId", request.getPolicyId())
                    .and(append("key", request.getKey()))
                    .and(append("path", request.getPath()))
                    .and(append("method", request.getMethod()))
                    .and(append("allowed", result.allowed()))
                    .and(append("limit", result.limit()))
                    .and(append("remaining", result.remaining()))
                    .and(append("resetAt", result.resetAt()))
                    .and(append("latencyMs", latencyMs));

            if (result.allowed()) {
                log.info(marker, "rate_limit_check");
            } else {
                log.warn(marker.and(append("retryAfter", result.retryAfter())), "rate_limit_throttled");
            }

            observer.onNext(ProtoMapper.toProto(result));
            observer.onCompleted();
        } catch (PolicyNotFoundException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asException());
        } catch (RateLimiterUnavailableException e) {
            observer.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void reloadConfig(ReloadConfigRequest request, StreamObserver<ReloadConfigResponse> observer) {
        try {
            policyLoader.reload(ProtoMapper.toPolicies(request.getPoliciesList()));
            observer.onNext(
                    ReloadConfigResponse.newBuilder()
                            .setSuccess(true)
                            .setPolicyCount(request.getPoliciesCount())
                            .setMessage("Reloaded policies")
                            .build()
            );
            observer.onCompleted();
        } catch (ConfigValidationException e) {
            String joined = String.join("; ", e.getErrors());
            observer.onError(Status.INVALID_ARGUMENT.withDescription(joined).asException());
        }
    }

    @Override
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> observer) {
        boolean storeOk = store.ping();
        observer.onNext(
                HealthCheckResponse.newBuilder()
                        .setStatus(storeOk ? "ok" : "degraded")
                        .setStoreOk(storeOk)
                        .build()
        );
        observer.onCompleted();
    }
}
