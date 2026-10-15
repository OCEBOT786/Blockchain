package com.supplychain.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SupabaseService {
    public record User(String id, String role, String walletAddress) {}

    private final String url;
    private final String publishableKey;
    private final RestTemplate http;

    @Autowired
    public SupabaseService(@Value("${SUPABASE_URL:}") String url,
                           @Value("${SUPABASE_PUBLISHABLE_KEY:}") String publishableKey) {
        this(url, publishableKey, new RestTemplate(new JdkClientHttpRequestFactory()));
    }

    SupabaseService(String url, String publishableKey, RestTemplate http) {
        this.url = url.replaceAll("/+$", "");
        this.publishableKey = publishableKey;
        this.http = http;
    }

    private void configured() {
        if (publishableKey.isBlank())
            throw new IllegalStateException("Supabase backend configuration is missing");
        try {
            URI base = URI.create(url);
            if (!("https".equals(base.getScheme()) || "http".equals(base.getScheme()))
                    || base.getHost() == null || (base.getRawPath() != null && !base.getRawPath().isEmpty())
                    || base.getRawQuery() != null || base.getRawFragment() != null)
                throw new IllegalStateException("Supabase backend URL is invalid");
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Supabase backend URL is invalid");
        }
    }

    private HttpHeaders headers(String token) {
        configured();
        if (token == null || token.isBlank()) throw new SecurityException("Supabase access token is required");
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", publishableKey);
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private URI uri(String path) {
        configured();
        return URI.create(url + path);
    }
    private static String enc(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(String path, String token) {
        ResponseEntity<List> response = http.exchange(uri(path), HttpMethod.GET, new HttpEntity<>(headers(token)), List.class);
        return (List<Map<String, Object>>) response.getBody();
    }

    public User authenticate(String token) {
        ResponseEntity<Map> response = http.exchange(uri("/auth/v1/user"), HttpMethod.GET,
                new HttpEntity<>(headers(token)), Map.class);
        Object idValue = response.getBody() == null ? null : response.getBody().get("id");
        if (!(idValue instanceof String id)) throw new SecurityException("Supabase did not authenticate this token");
        UUID.fromString(id);
        List<Map<String, Object>> profiles = rows("/rest/v1/profiles?select=id,role,wallet_address&id=eq." + enc(id), token);
        if (profiles == null || profiles.size() != 1 || !id.equals(profiles.get(0).get("id")))
            throw new SecurityException("Authenticated profile is unavailable");
        Map<String, Object> profile = profiles.get(0);
        return new User(id, (String) profile.get("role"), (String) profile.get("wallet_address"));
    }

    public Map<String, Object> getOrder(String token, UUID id) {
        List<Map<String, Object>> orders = rows("/rest/v1/orders?select=*&id=eq." + enc(id), token);
        if (orders == null || orders.isEmpty()) throw new IllegalArgumentException("Order was not found or is not accessible");
        return orders.get(0);
    }

    public List<Map<String, Object>> listOrders(String token) {
        List<Map<String, Object>> orders = rows("/rest/v1/orders?select=*&order=created_at.desc", token);
        return orders == null ? List.of() : orders;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> linkOrder(String token, UUID id, String expectedUpdatedAt,
                                          long chainOrderId, String txHash, String contractAddress) {
        if (expectedUpdatedAt == null) throw new IllegalArgumentException("Order has no updated_at timestamp");
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");
        String path = "/rest/v1/orders?id=eq." + enc(id) + "&blockchain_order_id=is.null&updated_at=eq." + enc(expectedUpdatedAt);
        Map<String, Object> payload = Map.of("blockchain_order_id", chainOrderId,
                "blockchain_tx_hash", txHash, "contract_address", contractAddress);
        ResponseEntity<List> response = http.exchange(uri(path), HttpMethod.PATCH,
                new HttpEntity<>(payload, headers), List.class);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getBody();
        if (rows == null || rows.size() != 1) throw new IllegalStateException("Order changed while linking; retry to recover the on-chain record");
        return rows.get(0);
    }
}
