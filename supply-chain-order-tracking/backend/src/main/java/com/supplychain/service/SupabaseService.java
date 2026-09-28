package com.supplychain.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

@Service
public class SupabaseService {
    // To be updated with details from Person 4
    private final String SUPABASE_URL = "YOUR_SUPABASE_URL/rest/v1/orders";
    private final String SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY";
    private final RestTemplate restTemplate = new RestTemplate();

    public void saveOrderMetadata(String orderId, Map<String, Object> metadata) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", SUPABASE_KEY);
        headers.set("Authorization", "Bearer " + SUPABASE_KEY);

        metadata.put("order_id", orderId);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(metadata, headers);
        
        restTemplate.postForEntity(SUPABASE_URL, request, String.class);
    }
}