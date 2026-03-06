package com.ratelimiter;

import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.grpc.LoggingInterceptor;
import com.ratelimiter.grpc.RateLimiterGrpcHandler;
import com.ratelimiter.model.FailMode;
import com.ratelimiter.store.RedisStoreAdapter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String redisUri = System.getenv().getOrDefault("REDIS_URI", "redis://localhost:6379");
        int grpcPort = Integer.parseInt(System.getenv().getOrDefault("GRPC_PORT", "50051"));
        FailMode failMode = FailMode.valueOf(System.getenv().getOrDefault("FAIL_MODE", "OPEN"));

        RedisStoreAdapter store = new RedisStoreAdapter(redisUri);
        PolicyLoader policyLoader = new PolicyLoader();
        RateLimiterService rateLimiterService = new RateLimiterService(policyLoader, store, failMode, null);
        RateLimiterGrpcHandler grpcHandler = new RateLimiterGrpcHandler(rateLimiterService, policyLoader, store);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Server server = ServerBuilder.forPort(grpcPort)
                .executor(executor)
                .addService(ServerInterceptors.intercept(grpcHandler, new LoggingInterceptor()))
                .build()
                .start();

        logger.info("gRPC server started on port {}", grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            server.shutdown();
            store.close();
            executor.shutdown();
        }));

        server.awaitTermination();
    }
}
