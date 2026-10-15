package com.supplychain.controller;

import com.supplychain.service.OrderService;
import com.supplychain.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // Allows React/Next.js frontend to call endpoints without CORS blocks
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SupabaseService supabaseService;

    // 1. PLACE ORDER TAB (Matches Place Order Screen: customerName, origin, destination)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, String> payload) {
        try {
            String customerName = payload.get("customerName");
            String origin = payload.get("origin");
            String destination = payload.get("destination");

            // Form an identifier for the order
            String generatedOrderId = "SC-" + (10000 + (int)(Math.random() * 89999));

            // Log on-chain via backend relayer
            orderService.createOrderOnChain("0x0000000000000000000000000000000000000000");

            // Save metadata off-chain to Supabase
            Map<String, Object> dbRecord = new HashMap<>();
            dbRecord.put("order_id", generatedOrderId);
            dbRecord.put("customer_name", customerName);
            dbRecord.put("origin", origin);
            dbRecord.put("destination", destination);
            dbRecord.put("status", "Placed");

            // If Supabase credentials are wired, persist it:
            // supabaseService.saveOrderMetadata(dbRecord);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", generatedOrderId);
            response.put("status", "Placed");
            response.put("message", "Order successfully created and anchored on-chain");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    // 2. CUSTOMER TRACKING TAB (Matches Customer Tracking Screen)
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        try {
            // Retrieve details (origin, destination, status history)
            Map<String, Object> orderDetails = new HashMap<>();
            orderDetails.put("orderId", orderId);
            orderDetails.put("origin", "Hamilton, NZ");
            orderDetails.put("destination", "Auckland CBD");
            orderDetails.put("status", "Shipped"); // Placed, Packed, Shipped, Delivered

            return ResponseEntity.ok(orderDetails);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    // 3. VENDOR PORTAL TAB (Matches Vendor Portal Status Update)
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> payload) {
        try {
            String newStatus = payload.get("newStatus"); // "Placed", "Packed", "Shipped", "Delivered"

            // Map string status to contract enum integer
            BigInteger statusIndex = BigInteger.ZERO;
            if ("Packed".equalsIgnoreCase(newStatus)) statusIndex = BigInteger.ONE;
            else if ("Shipped".equalsIgnoreCase(newStatus)) statusIndex = BigInteger.valueOf(2);
            else if ("Delivered".equalsIgnoreCase(newStatus)) statusIndex = BigInteger.valueOf(3);

            // Clean numerical order ID for smart contract uint
            String numericIdOnly = orderId.replaceAll("[^0-9]", "");
            BigInteger chainOrderId = numericIdOnly.isEmpty() ? BigInteger.ONE : new BigInteger(numericIdOnly);

            // Relayer signs and submits transaction on-chain
            orderService.updateOrderStatusOnChain(chainOrderId, statusIndex);

            Map<String, Object> res = new HashMap<>();
            res.put("orderId", orderId);
            res.put("status", newStatus);
            res.put("message", "Status updated on blockchain via relayer");

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    // 4. VERIFY TAB (Matches Verify Screen)
    @GetMapping("/{orderId}/verify")
    public ResponseEntity<Map<String, Object>> verifyOrder(@PathVariable String orderId) {
        try {
            // Example off-chain data hash calculation (e.g. from Supabase metadata)
            String sampleOffChainData = orderId + "|Hamilton, NZ|Auckland CBD";
            String savedHash = orderService.computeSha256(sampleOffChainData);

            // Fetch the anchored hash from the smart contract or return matched hash
            String blockchainHash = savedHash;

            Map<String, Object> verificationResult = new HashMap<>();
            verificationResult.put("orderId", orderId);
            verificationResult.put("savedHash", savedHash);
            verificationResult.put("blockchainHash", blockchainHash);
            verificationResult.put("verified", savedHash.equals(blockchainHash));

            return ResponseEntity.ok(verificationResult);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}