package com.ratelimiter.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.ratelimiter.RateLimiterService;
import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.grpc.proto.CheckRateLimitRequest;
import com.ratelimiter.grpc.proto.CheckRateLimitResponse;
import com.ratelimiter.grpc.proto.HealthCheckRequest;
import com.ratelimiter.grpc.proto.HealthCheckResponse;
import com.ratelimiter.grpc.proto.ReloadConfigRequest;
import com.ratelimiter.model.Errors.ConfigValidationException;
import com.ratelimiter.model.Errors.PolicyNotFoundException;
import com.ratelimiter.model.Errors.RateLimiterUnavailableException;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.store.StoreAdapter;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RateLimiterGrpcHandlerTest {

    @Test
    void checkRateLimitSuccessCallsOnCompleted() {
        RateLimiterService service = mock(RateLimiterService.class);
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        when(service.check(any())).thenReturn(new RateLimitResult(true, 100, 99, 1234L, 0));

        RateLimiterGrpcHandler handler = new RateLimiterGrpcHandler(service, loader, store);
        @SuppressWarnings("unchecked")
        StreamObserver<CheckRateLimitResponse> observer = mock(StreamObserver.class);

        handler.checkRateLimit(CheckRateLimitRequest.newBuilder().setPolicyId("p").setKey("k").build(), observer);

        verify(observer).onNext(any(CheckRateLimitResponse.class));
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
    }

    @Test
    void checkRateLimitPolicyMissingMapsToNotFound() {
        RateLimiterService service = mock(RateLimiterService.class);
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        when(service.check(any())).thenThrow(new PolicyNotFoundException("no policy"));

        RateLimiterGrpcHandler handler = new RateLimiterGrpcHandler(service, loader, store);
        @SuppressWarnings("unchecked")
        StreamObserver<CheckRateLimitResponse> observer = mock(StreamObserver.class);

        handler.checkRateLimit(CheckRateLimitRequest.newBuilder().build(), observer);

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(throwable.capture());
        assertEquals(Status.NOT_FOUND.getCode(), Status.fromThrowable(throwable.getValue()).getCode());
    }

    @Test
    void checkRateLimitUnavailableMapsToUnavailable() {
        RateLimiterService service = mock(RateLimiterService.class);
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        when(service.check(any())).thenThrow(new RateLimiterUnavailableException("down"));

        RateLimiterGrpcHandler handler = new RateLimiterGrpcHandler(service, loader, store);
        @SuppressWarnings("unchecked")
        StreamObserver<CheckRateLimitResponse> observer = mock(StreamObserver.class);

        handler.checkRateLimit(CheckRateLimitRequest.newBuilder().build(), observer);

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(throwable.capture());
        assertEquals(Status.UNAVAILABLE.getCode(), Status.fromThrowable(throwable.getValue()).getCode());
    }

    @Test
    void reloadConfigValidationFailureMapsToInvalidArgument() {
        RateLimiterService service = mock(RateLimiterService.class);
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);

        ConfigValidationException validationException = new ConfigValidationException(List.of("limit invalid"));
        doThrow(validationException).when(loader).reload(any());

        RateLimiterGrpcHandler handler = new RateLimiterGrpcHandler(service, loader, store);
        @SuppressWarnings("unchecked")
        StreamObserver<com.ratelimiter.grpc.proto.ReloadConfigResponse> observer = mock(StreamObserver.class);

        handler.reloadConfig(ReloadConfigRequest.newBuilder().build(), observer);

        ArgumentCaptor<Throwable> throwable = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(throwable.capture());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), Status.fromThrowable(throwable.getValue()).getCode());
    }

    @Test
    void healthCheckReturnsOkWhenStorePingSucceeds() {
        RateLimiterService service = mock(RateLimiterService.class);
        PolicyLoader loader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        when(store.ping()).thenReturn(true);

        RateLimiterGrpcHandler handler = new RateLimiterGrpcHandler(service, loader, store);
        @SuppressWarnings("unchecked")
        StreamObserver<HealthCheckResponse> observer = mock(StreamObserver.class);

        handler.healthCheck(HealthCheckRequest.newBuilder().build(), observer);

        ArgumentCaptor<HealthCheckResponse> captor = ArgumentCaptor.forClass(HealthCheckResponse.class);
        verify(observer).onNext(captor.capture());
        assertEquals("ok", captor.getValue().getStatus());
        assertEquals(true, captor.getValue().getStoreOk());
    }
}
