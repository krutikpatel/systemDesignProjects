package com.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.grpc.RateLimiterGrpcHandler;
import com.ratelimiter.grpc.proto.HealthCheckRequest;
import com.ratelimiter.grpc.proto.HealthCheckResponse;
import com.ratelimiter.grpc.proto.RateLimiterServiceGrpc;
import com.ratelimiter.store.StoreAdapter;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ServerSmokeTest {
    private Server server;
    private ManagedChannel channel;
    private ExecutorService executor;

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void inProcessHealthCheckReturnsOk() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        PolicyLoader policyLoader = mock(PolicyLoader.class);
        StoreAdapter store = mock(StoreAdapter.class);
        when(store.ping()).thenReturn(true);

        RateLimiterGrpcHandler handler = new RateLimiterGrpcHandler(rateLimiterService, policyLoader, store);

        executor = Executors.newVirtualThreadPerTaskExecutor();
        server = InProcessServerBuilder.forName(serverName)
                .executor(executor)
                .addService(handler)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).build();

        HealthCheckResponse response = RateLimiterServiceGrpc.newBlockingStub(channel)
                .healthCheck(HealthCheckRequest.newBuilder().build());

        assertEquals("ok", response.getStatus());
        assertTrue(response.getStoreOk());
    }

    @Test
    void rpcRunsOnVirtualThread() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        AtomicBoolean sawVirtualThread = new AtomicBoolean(false);

        RateLimiterServiceGrpc.RateLimiterServiceImplBase service = new RateLimiterServiceGrpc.RateLimiterServiceImplBase() {
            @Override
            public void healthCheck(HealthCheckRequest request,
                                    io.grpc.stub.StreamObserver<HealthCheckResponse> responseObserver) {
                sawVirtualThread.set(Thread.currentThread().isVirtual());
                responseObserver.onNext(HealthCheckResponse.newBuilder().setStatus("ok").setStoreOk(true).build());
                responseObserver.onCompleted();
            }
        };

        executor = Executors.newVirtualThreadPerTaskExecutor();
        server = InProcessServerBuilder.forName(serverName)
                .executor(executor)
                .addService(service)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).build();

        HealthCheckResponse response = RateLimiterServiceGrpc.newBlockingStub(channel)
                .healthCheck(HealthCheckRequest.newBuilder().build());

        assertEquals("ok", response.getStatus());
        assertTrue(sawVirtualThread.get());
    }
}
