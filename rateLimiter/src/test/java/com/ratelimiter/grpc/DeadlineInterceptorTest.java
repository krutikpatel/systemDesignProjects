package com.ratelimiter.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.grpc.proto.HealthCheckRequest;
import com.ratelimiter.grpc.proto.HealthCheckResponse;
import com.ratelimiter.grpc.proto.RateLimiterServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DeadlineInterceptorTest {
    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void expiredDeadlineReturnsDeadlineExceededAndSkipsService() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(calls);

        RateLimiterServiceGrpc.RateLimiterServiceBlockingStub stub = RateLimiterServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(0, TimeUnit.MILLISECONDS);

        StatusRuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                StatusRuntimeException.class,
                () -> stub.healthCheck(HealthCheckRequest.newBuilder().build())
        );

        assertEquals(Status.DEADLINE_EXCEEDED.getCode(), ex.getStatus().getCode());
        assertEquals(0, calls.get());
    }

    @Test
    void sufficientDeadlineProceedsNormally() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(calls);

        HealthCheckResponse response = RateLimiterServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(1, TimeUnit.SECONDS)
                .healthCheck(HealthCheckRequest.newBuilder().build());

        assertEquals("ok", response.getStatus());
        assertTrue(calls.get() > 0);
    }

    @Test
    void noDeadlineProceedsNormally() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(calls);

        HealthCheckResponse response = RateLimiterServiceGrpc.newBlockingStub(channel)
                .healthCheck(HealthCheckRequest.newBuilder().build());

        assertEquals("ok", response.getStatus());
        assertTrue(calls.get() > 0);
    }

    private void startServer(AtomicInteger calls) throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        RateLimiterServiceGrpc.RateLimiterServiceImplBase service = new RateLimiterServiceGrpc.RateLimiterServiceImplBase() {
            @Override
            public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
                calls.incrementAndGet();
                responseObserver.onNext(HealthCheckResponse.newBuilder().setStatus("ok").setStoreOk(true).build());
                responseObserver.onCompleted();
            }
        };

        server = InProcessServerBuilder.forName(serverName)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .addService(ServerInterceptors.intercept(service, new DeadlineInterceptor()))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).build();
    }
}
