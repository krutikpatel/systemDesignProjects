package com.ratelimiter.metrics;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MetricsHttpServer {
    private static final String HEALTH_BODY = "{\"status\":\"ok\"}";

    private final MetricsRegistry registry;
    private final int port;

    private HttpServer server;
    private ExecutorService executor;

    public MetricsHttpServer(MetricsRegistry registry) {
        this(registry, Integer.parseInt(System.getenv().getOrDefault("METRICS_PORT", "9090")));
    }

    public MetricsHttpServer(MetricsRegistry registry, int port) {
        this.registry = registry;
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", new MetricsHandler(registry));
        server.createContext("/healthz", new HealthHandler());

        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.start();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    ExecutorService executorForTest() {
        return executor;
    }

    private static final class MetricsHandler implements HttpHandler {
        private final MetricsRegistry registry;

        private MetricsHandler(MetricsRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = registry.scrape().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = HEALTH_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
