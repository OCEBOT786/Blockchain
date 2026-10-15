package com.supplychain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_BLOCKCHAIN_TEST", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IsolatedWorkflowTest {
    private static final String BUYER = "87654321-4321-4321-8321-cba987654321";
    private static final String LOGISTICS = "97654321-4321-4321-8321-cba987654321";
    private static final UUID ID = UUID.randomUUID();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, Object> ORDER = new HashMap<>();
    private static HttpServer server;

    static {
        ORDER.put("id", ID.toString()); ORDER.put("buyer_id", BUYER);
        ORDER.put("logistics_provider_id", LOGISTICS);
        ORDER.put("product_name", "Coffee Beans"); ORDER.put("product_description", "Premium coffee");
        ORDER.put("quantity", 20); ORDER.put("delivery_address", "123 Test Street, Auckland");
        ORDER.put("blockchain_order_id", null); ORDER.put("blockchain_tx_hash", null);
        ORDER.put("contract_address", null); ORDER.put("updated_at", "2026-10-16T00:00:00Z");
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", IsolatedWorkflowTest::handle);
        server.start();
        registry.add("SUPABASE_URL", () -> "http://127.0.0.1:" + server.getAddress().getPort());
        registry.add("SUPABASE_PUBLISHABLE_KEY", () -> "public-test-key");
    }

    @AfterAll static void stop() { if (server != null) server.stop(0); }

    private static String query(HttpExchange exchange, String key) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null) return null;
        for (String part : raw.split("&")) {
            String[] pieces = part.split("=", 2);
            if (pieces[0].equals(key)) return URLDecoder.decode(pieces.length == 2 ? pieces[1] : "", StandardCharsets.UTF_8);
        }
        return null;
    }

    private static void send(HttpExchange exchange, int code, Object body) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static synchronized void handle(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        String id = "Bearer buyer-token".equals(token) ? BUYER : "Bearer logistics-token".equals(token) ? LOGISTICS : null;
        if (id == null) { send(exchange, 401, Map.of("message", "Invalid token")); return; }
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/auth/v1/user")) { send(exchange, 200, Map.of("id", id)); return; }
        if (path.equals("/rest/v1/profiles")) {
            if (!("eq." + id).equals(query(exchange, "id"))) { send(exchange, 200, java.util.List.of()); return; }
            send(exchange, 200, java.util.List.of(Map.of("id", id, "role", id.equals(BUYER) ? "buyer" : "logistics_provider",
                    "wallet_address", id.equals(BUYER) ? System.getenv("BLOCKCHAIN_CUSTOMER_ADDRESS") : "")));
            return;
        }
        if (path.equals("/rest/v1/orders")) {
            if ("PATCH".equals(exchange.getRequestMethod())) {
                if (!id.equals(BUYER) || ORDER.get("blockchain_order_id") != null ||
                        !("eq." + ORDER.get("updated_at")).equals(query(exchange, "updated_at"))) {
                    send(exchange, 200, java.util.List.of()); return;
                }
                @SuppressWarnings("unchecked") Map<String, Object> update = JSON.readValue(exchange.getRequestBody(), Map.class);
                ORDER.putAll(update);
                send(exchange, 200, java.util.List.of(new HashMap<>(ORDER)));
                return;
            }
            String filter = query(exchange, "id");
            send(exchange, 200, filter == null || ("eq." + ID).equals(filter)
                    ? java.util.List.of(new HashMap<>(ORDER)) : java.util.List.of());
            return;
        }
        send(exchange, 404, Map.of("error", "Unknown mock endpoint"));
    }

    private static HttpEntity<?> request(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (body != null) headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Autowired TestRestTemplate http;

    @Test void buyerToLogisticsWorkflowAndTamperDetection() {
        String base = "/api/orders/" + ID;
        assertEquals(HttpStatus.FORBIDDEN, http.exchange(base + "/link", HttpMethod.POST,
                request("logistics-token", null), Map.class).getStatusCode());
        ResponseEntity<Map> linked = http.exchange(base + "/link", HttpMethod.POST, request("buyer-token", null), Map.class);
        assertEquals(HttpStatus.OK, linked.getStatusCode(), String.valueOf(linked.getBody()));
        Object chainId = linked.getBody().get("blockchain_order_id");
        assertTrue(new BigInteger(String.valueOf(chainId)).signum() > 0);
        assertTrue(String.valueOf(linked.getBody().get("blockchain_tx_hash")).matches("0x[0-9a-fA-F]{64}"));
        assertEquals(chainId, http.exchange(base + "/link", HttpMethod.POST,
                request("buyer-token", null), Map.class).getBody().get("blockchain_order_id"));
        assertEquals("VERIFIED", http.exchange(base + "/verify", HttpMethod.GET,
                request("buyer-token", null), Map.class).getBody().get("status"));
        assertEquals(0, ((Number) http.exchange(base, HttpMethod.GET,
                request("buyer-token", null), Map.class).getBody().get("onchain_status")).intValue());
        assertEquals(HttpStatus.FORBIDDEN, http.exchange(base + "/status", HttpMethod.PUT,
                request("buyer-token", Map.of("status", 1)), Map.class).getStatusCode());
        assertEquals(1, ((Number) http.exchange(base + "/status", HttpMethod.PUT,
                request("logistics-token", Map.of("status", 1)), Map.class).getBody().get("onchain_status")).intValue());
        assertEquals(1, ((Number) http.exchange(base, HttpMethod.GET,
                request("buyer-token", null), Map.class).getBody().get("onchain_status")).intValue());
        ORDER.put("quantity", 21); // Controlled mock corruption; a real migrated database rejects this update.
        assertEquals("MISMATCH", http.exchange(base + "/verify", HttpMethod.GET,
                request("buyer-token", null), Map.class).getBody().get("status"));
    }
}
