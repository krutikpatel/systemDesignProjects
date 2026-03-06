package com.ratelimiter.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ratelimiter.model.Algorithm;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class MetricsRegistryTest {

    @Test
    void requestCountersTrackAllowedAndThrottled() {
        MetricsRegistry registry = MetricsRegistry.simple();

        for (int i = 0; i < 10; i++) {
            registry.recordRequest("policy-1", Algorithm.TOKEN_BUCKET, true);
        }
        for (int i = 0; i < 5; i++) {
            registry.recordRequest("policy-1", Algorithm.TOKEN_BUCKET, false);
        }

        double allowed = registry.meterRegistry()
                .get("drl.requests.total")
                .tag("result", "allowed")
                .counter()
                .count();
        double throttled = registry.meterRegistry()
                .get("drl.requests.total")
                .tag("result", "throttled")
                .counter()
                .count();

        assertEquals(10.0, allowed);
        assertEquals(5.0, throttled);
    }

    @Test
    void metricsAndHealthEndpointsAreServed() throws Exception {
        MetricsRegistry registry = new MetricsRegistry();
        registry.recordRequest("policy-a", Algorithm.TOKEN_BUCKET, true);

        int port = 19090;
        MetricsHttpServer server = new MetricsHttpServer(registry, port);
        try {
            server.start();

            String metricsBody = httpGet("http://localhost:" + port + "/metrics");
            String healthBody = httpGet("http://localhost:" + port + "/healthz");

            assertTrue(metricsBody.contains("drl_requests_total"));
            assertEquals("{\"status\":\"ok\"}", healthBody);

            Future<Boolean> future = server.executorForTest().submit(() -> Thread.currentThread().isVirtual());
            assertTrue(future.get());
        } finally {
            server.stop();
        }
    }

    private static String httpGet(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
    }
}
