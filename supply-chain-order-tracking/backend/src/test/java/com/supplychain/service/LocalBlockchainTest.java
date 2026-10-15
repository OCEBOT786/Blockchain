package com.supplychain.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

class LocalBlockchainTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_LOCAL_BLOCKCHAIN_TEST", matches = "true")
    void createsRecoversAndUpdatesFreshLocalContract() throws Exception {
        String address = System.getenv("BLOCKCHAIN_CONTRACT_ADDRESS");
        String customer = System.getenv("BLOCKCHAIN_CUSTOMER_ADDRESS");
        assertNotNull(address);
        assertNotNull(customer);
        Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:8545"));
        try {
            OrderService service = new OrderService(web3j, address, BigInteger.valueOf(31337),
                    System.getenv("BLOCKCHAIN_OWNER_PRIVATE_KEY"),
                    System.getenv("BLOCKCHAIN_OPERATOR_PRIVATE_KEY"));
            Map<String, Object> row = new HashMap<>();
            row.put("id", UUID.randomUUID().toString());
            row.put("buyer_id", UUID.randomUUID().toString());
            row.put("product_name", "Coffee Beans");
            row.put("product_description", "Premium coffee");
            row.put("quantity", 20);
            row.put("delivery_address", "123 Test Street, Auckland");
            String hash = new OrderMetadata().hash(row);
            assertEquals(BigInteger.ZERO, service.findByHash(hash));
            BlockchainGateway.Created created = service.createOrder(customer, hash);
            assertEquals(hash, created.order().metadataHash());
            assertEquals(customer.toLowerCase(), created.order().customer().toLowerCase());
            assertTrue(created.transactionHash().matches("0x[0-9a-fA-F]{64}"));
            assertEquals(created.order().id(), service.findByHash(hash));
            BlockchainGateway.Created recovered = service.recoverOrder(created.order().id(), hash);
            assertEquals(created.transactionHash(), recovered.transactionHash());
            assertEquals(1, service.updateStatus(created.order().id(), 1).status());
            assertEquals(2, service.updateStatus(created.order().id(), 2).status());
            assertEquals(hash, service.getOrder(created.order().id()).metadataHash());
        } finally {
            web3j.shutdown();
        }
    }
}
