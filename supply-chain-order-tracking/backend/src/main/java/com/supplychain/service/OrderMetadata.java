package com.supplychain.service;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;

@Component
public class OrderMetadata {
    public static final String VERSION = "SCOT_ORDER_METADATA_V1";

    private static byte[] uuidBytes(Object input, String field) {
        if (!(input instanceof String text)) throw new IllegalArgumentException(field + " must be a UUID");
        UUID uuid = UUID.fromString(text);
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    private static String requiredText(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (!(value instanceof String text) || text.isEmpty()) throw new IllegalArgumentException(key + " is required");
        return text;
    }

    private static String nullableText(Map<String, Object> row, String key) {
        if (!row.containsKey(key)) throw new IllegalArgumentException(key + " is missing");
        Object value = row.get(key);
        if (value != null && !(value instanceof String)) throw new IllegalArgumentException(key + " must be text or null");
        return value == null ? "" : (String) value;
    }

    public String hash(Map<String, Object> row) {
        String product = requiredText(row, "product_name");
        String description = nullableText(row, "product_description");
        String address = nullableText(row, "delivery_address");
        Object quantityValue = row.get("quantity");
        if (!(quantityValue instanceof Number number)) throw new IllegalArgumentException("quantity must be a positive integer");
        long quantity = number.longValue();
        if (quantity <= 0 || quantity > 9_007_199_254_740_991L || number.doubleValue() != quantity)
            throw new IllegalArgumentException("quantity must be a positive safe integer");
        var values = Arrays.<Type>asList(
                new Utf8String(VERSION),
                new Bytes16(uuidBytes(row.get("id"), "id")),
                new Bytes16(uuidBytes(row.get("buyer_id"), "buyer_id")),
                new Utf8String(product),
                new Bool(row.get("product_description") != null),
                new Utf8String(description),
                new Uint256(quantity),
                new Bool(row.get("delivery_address") != null),
                new Utf8String(address));
        return Hash.sha3(FunctionEncoder.encodeConstructor(values));
    }
}
