package com.ratelimiter.integration;

import com.ratelimiter.RateLimiterService;
import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.grpc.RateLimiterGrpcHandler;
import com.ratelimiter.grpc.proto.RateLimiterServiceGrpc;
import com.ratelimiter.model.FailMode;
import com.ratelimiter.store.RedisStoreAdapter;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
abstract class IntegrationTestBase {
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    protected RedisStoreAdapter store;
    protected PolicyLoader policyLoader;
    protected RateLimiterService rateLimiterService;
    protected RateLimiterServiceGrpc.RateLimiterServiceBlockingStub grpcStub;

    private Server server;
    private ManagedChannel channel;
    private ExecutorService executor;

    @BeforeEach
    void setUpBase() throws IOException {
        String redisUri = "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379);

        store = new RedisStoreAdapter(redisUri);
        policyLoader = new PolicyLoader();
        rateLimiterService = new RateLimiterService(policyLoader, store, FailMode.CLOSED, null);

        String name = InProcessServerBuilder.generateName();
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server = InProcessServerBuilder.forName(name)
                .executor(executor)
                .addService(new RateLimiterGrpcHandler(rateLimiterService, policyLoader, store))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(name).build();
        grpcStub = RateLimiterServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDownBase() {
        flushRedis();
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (store != null) {
            store.close();
        }
    }

    private void flushRedis() {
        String redisUri = "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379);
        RedisClient client = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> conn = client.connect();
        try {
            conn.sync().flushdb();
        } finally {
            conn.close();
            client.shutdown();
        }
    }
}
