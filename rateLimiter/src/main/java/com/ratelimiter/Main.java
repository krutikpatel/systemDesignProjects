package com.ratelimiter;

import com.ratelimiter.config.PolicyLoader;
import com.ratelimiter.grpc.DeadlineInterceptor;
import com.ratelimiter.grpc.LoggingInterceptor;
import com.ratelimiter.grpc.RateLimiterGrpcHandler;
import com.ratelimiter.metrics.MetricsHttpServer;
import com.ratelimiter.metrics.MetricsRegistry;
import com.ratelimiter.model.FailMode;
import com.ratelimiter.store.CircuitBreakerStoreAdapter;
import com.ratelimiter.store.RedisStoreAdapter;
import com.ratelimiter.store.StoreAdapter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_REDIS_URI = "redis://localhost:6379";
    private static final int DEFAULT_GRPC_PORT = 50051;
    private static final int DEFAULT_METRICS_PORT = 9090;
    private static final FailMode DEFAULT_FAIL_MODE = FailMode.OPEN;

    public static void main(String[] args) throws Exception {
        RuntimeConfig config = RuntimeConfig.fromEnv(System.getenv());
        logger.info(
                "Starting DRL with redisUri={}, grpcPort={}, metricsPort={}, failMode={}",
                config.redisUri(),
                config.grpcPort(),
                config.metricsPort(),
                config.failMode()
        );

        MetricsRegistry metricsRegistry = new MetricsRegistry();
        MetricsHttpServer metricsServer = new MetricsHttpServer(metricsRegistry, config.metricsPort());
        startMetricsServer(metricsServer);

        StoreAdapter store = buildStore(config.redisUri(), metricsRegistry);
        PolicyLoader policyLoader = new PolicyLoader();
        RateLimiterService rateLimiterService = new RateLimiterService(
                policyLoader,
                store,
                config.failMode(),
                null,
                metricsRegistry
        );
        RateLimiterGrpcHandler grpcHandler = new RateLimiterGrpcHandler(rateLimiterService, policyLoader, store);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Server server = ServerBuilder.forPort(config.grpcPort())
                .executor(executor)
                .addService(ServerInterceptors.intercept(grpcHandler, defaultInterceptors()))
                .build()
                .start();

        logger.info("gRPC server started on port {}", config.grpcPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            server.shutdown();
            metricsServer.stop();
            store.close();
            executor.shutdown();
        }));

        server.awaitTermination();
    }

    static StoreAdapter buildStore(String redisUri, MetricsRegistry metricsRegistry) {
        return wrapWithCircuitBreaker(new RedisStoreAdapter(redisUri, metricsRegistry));
    }

    static StoreAdapter wrapWithCircuitBreaker(StoreAdapter delegate) {
        return new CircuitBreakerStoreAdapter(delegate);
    }

    static ServerInterceptor[] defaultInterceptors() {
        List<ServerInterceptor> interceptors = List.of(new DeadlineInterceptor(), new LoggingInterceptor());
        return interceptors.toArray(ServerInterceptor[]::new);
    }

    private static void startMetricsServer(MetricsHttpServer metricsServer) {
        try {
            metricsServer.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start metrics HTTP server", e);
        }
    }

    record RuntimeConfig(String redisUri, int grpcPort, int metricsPort, FailMode failMode) {
        static RuntimeConfig fromEnv(Map<String, String> env) {
            String redisUri = env.getOrDefault("REDIS_URI", DEFAULT_REDIS_URI);
            int grpcPort = parsePort("GRPC_PORT", env.get("GRPC_PORT"), DEFAULT_GRPC_PORT);
            int metricsPort = parsePort("METRICS_PORT", env.get("METRICS_PORT"), DEFAULT_METRICS_PORT);
            FailMode failMode = parseFailMode(env.get("FAIL_MODE"));
            return new RuntimeConfig(redisUri, grpcPort, metricsPort, failMode);
        }

        private static int parsePort(String key, String raw, int defaultValue) {
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }
            try {
                int parsed = Integer.parseInt(raw);
                if (parsed <= 0 || parsed > 65_535) {
                    throw new IllegalArgumentException(key + " must be in range 1..65535");
                }
                return parsed;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(key + " must be an integer", ex);
            }
        }

        private static FailMode parseFailMode(String raw) {
            if (raw == null || raw.isBlank()) {
                return DEFAULT_FAIL_MODE;
            }
            try {
                return FailMode.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("FAIL_MODE must be OPEN or CLOSED", ex);
            }
        }
    }
}
