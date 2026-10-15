package com.supplychain.controller;

import com.supplychain.service.OrderWorkflow;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderWorkflow workflow;
    public OrderController(OrderWorkflow workflow) { this.workflow = workflow; }

    private static String token(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7)
            throw new SecurityException("Supabase bearer token is required");
        return header.substring(7);
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestHeader(value = "Authorization", required = false) String authorization) throws Exception {
        return workflow.list(token(authorization));
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable UUID id) throws Exception {
        return workflow.get(token(authorization), id);
    }

    @PostMapping("/{id}/link")
    public Map<String, Object> link(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable UUID id) throws Exception {
        return workflow.link(token(authorization), id);
    }

    @GetMapping("/{id}/verify")
    public Map<String, Object> verify(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        return workflow.verify(token(authorization), id);
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id, @RequestBody Map<String, Integer> body) throws Exception {
        Integer status = body.get("status");
        if (status == null || (status != 1 && status != 2)) throw new IllegalArgumentException("Status must be 1 or 2");
        return workflow.update(token(authorization), id, status);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> forbidden(SecurityException error) {
        HttpStatus status = error.getMessage().contains("token") ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<Map<String, String>> upstream(HttpStatusCodeException error) {
        HttpStatus status = error.getStatusCode().value() == 401 ? HttpStatus.UNAUTHORIZED
                : error.getStatusCode().value() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", "Supabase request failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> failure(Exception error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                "Order operation failed. Retry or check the server configuration."));
    }
}
