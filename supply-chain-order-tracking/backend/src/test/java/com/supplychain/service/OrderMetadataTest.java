package com.supplychain.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderMetadataTest {
    @Test void matchesJavascriptVectorAndDetectsMutation() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", "12345678-1234-4234-8234-123456789abc");
        row.put("buyer_id", "87654321-4321-4321-8321-cba987654321");
        row.put("product_name", "Coffee Beans");
        row.put("product_description", "Premium coffee");
        row.put("quantity", 20);
        row.put("delivery_address", "123 Test Street, Auckland");
        OrderMetadata metadata = new OrderMetadata();
        String expected = "0x950baa1d77b7153a7753d89cfa5ed90404e79acca9b4f0a56aeee74c43ccc3b9";
        assertEquals(expected, metadata.hash(row));
        row.put("quantity", 21);
        assertNotEquals(expected, metadata.hash(row));
        row.put("quantity", 20);
        assertEquals(expected, metadata.hash(row));
        row.put("product_description", null);
        String nullHash = metadata.hash(row);
        row.put("product_description", "");
        assertNotEquals(nullHash, metadata.hash(row));
    }
}
