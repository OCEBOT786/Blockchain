package com.supplychain.service;

import com.supplychain.contract.SupplyChainOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private Web3j web3j;

    // Use Account #0 or your Sepolia/Local relayer credentials
    private final String PRIVATE_KEY = "YOUR_HARDHAT_OR_SEPOLIA_PRIVATE_KEY";
    private final String CONTRACT_ADDRESS = "YOUR_DEPLOYED_CONTRACT_ADDRESS";

    private SupplyChainOrder getContract() {
        Credentials credentials = Credentials.create(PRIVATE_KEY);
        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(4_300_000);
        return SupplyChainOrder.load(
                CONTRACT_ADDRESS,
                web3j,
                credentials,
                new StaticGasProvider(gasPrice, gasLimit)
        );
    }

    public TransactionReceipt createOrderOnChain(String customerAddress) throws Exception {
        SupplyChainOrder contract = getContract();
        return contract.createOrder(customerAddress).send();
    }

    public TransactionReceipt updateOrderStatusOnChain(BigInteger orderId, BigInteger status) throws Exception {
        SupplyChainOrder contract = getContract();
        return contract.updateOrderStatus(orderId, status).send();
    }

    public Map<String, Object> getOrderFromChain(BigInteger orderId) throws Exception {
        SupplyChainOrder contract = getContract();
        // Calls the order mapping/getter on the contract
        // Adjust the tuple accessors according to your contract's getOrder / orders mapping return
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        return orderData;
    }

    public String computeSha256(String rawData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "0x" + hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing data", e);
        }
    }
}