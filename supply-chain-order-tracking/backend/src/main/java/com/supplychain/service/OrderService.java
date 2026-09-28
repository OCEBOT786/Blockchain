package com.supplychain.service;

import com.supplychain.contract.SupplyChainOrder;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import java.math.BigInteger;

@Service
public class OrderService {
    private final SupplyChainOrder contract;

    public OrderService(Web3j web3j) {
        // Retrieve from your Hardhat local node deployment
        Credentials credentials = Credentials.create("YOUR_HARDHAT_PRIVATE_KEY");
        this.contract = SupplyChainOrder.load(
            "YOUR_DEPLOYED_CONTRACT_ADDRESS", web3j, credentials, new DefaultGasProvider()
        );
    }

    public void createOrderOnChain(String customerAddress) throws Exception {
        contract.createOrder(customerAddress).send();
    }

    public void updateStatus(BigInteger orderId, BigInteger statusEnumIndex) throws Exception {
        contract.updateOrderStatus(orderId, statusEnumIndex).send();
    }
}