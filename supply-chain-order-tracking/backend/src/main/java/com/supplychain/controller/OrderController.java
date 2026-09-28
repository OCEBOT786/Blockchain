package com.supplychain.controller;

import com.supplychain.service.OrderService;
import com.supplychain.service.SupabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    private final SupabaseService supabaseService;

    public OrderController(OrderService orderService, SupabaseService supabaseService) {
        this.orderService = orderService;
        this.supabaseService = supabaseService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            String customerAddress = (String) payload.get("customerAddress");
            
            // 1. Log immutable transaction on Blockchain
            orderService.createOrderOnChain(customerAddress);
            
            // 2. Save heavy metadata off-chain in Supabase
            supabaseService.saveOrderMetadata(customerAddress, payload);
            
            return ResponseEntity.ok("Order securely created and metadata saved.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable BigInteger id, @RequestParam BigInteger statusIndex) {
        try {
            // Trigger smart contract state change (0 = Received, 1 = InTransit, 2 = Delivered)
            orderService.updateStatus(id, statusIndex);
            return ResponseEntity.ok("Order milestone updated on blockchain.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}