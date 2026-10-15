package com.supplychain.service;

import java.math.BigInteger;

public interface BlockchainGateway {
    record ChainOrder(BigInteger id, String customer, int status, BigInteger timestamp, String metadataHash) {}
    record Created(ChainOrder order, String transactionHash) {}
    String contractAddress();
    BigInteger chainId();
    BigInteger findByHash(String hash) throws Exception;
    ChainOrder getOrder(BigInteger id) throws Exception;
    Created createOrder(String customer, String hash) throws Exception;
    Created recoverOrder(BigInteger id, String hash) throws Exception;
    ChainOrder updateStatus(BigInteger id, int status) throws Exception;
}
