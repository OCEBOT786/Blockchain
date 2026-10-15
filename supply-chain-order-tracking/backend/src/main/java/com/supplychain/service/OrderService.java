package com.supplychain.service;

import com.supplychain.contract.SupplyChainOrder;
import java.math.BigInteger;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

@Service
public class OrderService implements BlockchainGateway {
    private final Web3j web3j;
    private final String contractAddress;
    private final BigInteger configuredChainId;
    private final String ownerKey;
    private final String operatorKey;
    private final StaticGasProvider gas = new StaticGasProvider(BigInteger.valueOf(20_000_000_000L), BigInteger.valueOf(4_300_000));

    public OrderService(Web3j web3j,
            @Value("${BLOCKCHAIN_CONTRACT_ADDRESS:}") String contractAddress,
            @Value("${BLOCKCHAIN_CHAIN_ID:31337}") BigInteger configuredChainId,
            @Value("${BLOCKCHAIN_OWNER_PRIVATE_KEY:}") String ownerKey,
            @Value("${BLOCKCHAIN_OPERATOR_PRIVATE_KEY:}") String operatorKey) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
        this.configuredChainId = configuredChainId;
        this.ownerKey = ownerKey;
        this.operatorKey = operatorKey;
    }

    @Override public String contractAddress() { return contractAddress; }
    @Override public BigInteger chainId() { return configuredChainId; }

    private void trustedChain() throws Exception {
        if (!contractAddress.matches("0x[0-9a-fA-F]{40}")) throw new IllegalStateException("Integrity contract address is not configured");
        if (!web3j.ethChainId().send().getChainId().equals(configuredChainId))
            throw new IllegalStateException("Blockchain RPC has the wrong chain ID");
        String code = web3j.ethGetCode(contractAddress, DefaultBlockParameterName.LATEST).send().getCode();
        if (code == null || code.equals("0x") || code.equals("0x0"))
            throw new IllegalStateException("No contract code at the configured address");
    }

    private SupplyChainOrder readContract() throws Exception {
        trustedChain();
        return SupplyChainOrder.load(contractAddress, web3j, Credentials.create(Keys.createEcKeyPair()), gas);
    }

    private SupplyChainOrder signingContract(String key, boolean owner) throws Exception {
        if (key.isBlank()) throw new IllegalStateException("Blockchain signing wallet is not configured");
        trustedChain();
        Credentials credentials = Credentials.create(key);
        SupplyChainOrder contract = SupplyChainOrder.load(contractAddress, web3j, credentials, gas);
        if (owner && !contract.owner().send().equalsIgnoreCase(credentials.getAddress()))
            throw new SecurityException("Configured owner wallet does not own the contract");
        if (!owner && !contract.authorizedOperators(credentials.getAddress()).send())
            throw new SecurityException("Configured operator wallet is not authorized by the contract");
        return contract;
    }

    @Override public BigInteger findByHash(String hash) throws Exception {
        return readContract().orderIdByMetadataHash(hash).send();
    }

    @Override public ChainOrder getOrder(BigInteger id) throws Exception {
        Tuple5<BigInteger, String, BigInteger, BigInteger, String> tuple = readContract().getOrder(id).send();
        return new ChainOrder(tuple.component1(), tuple.component2(), tuple.component3().intValueExact(),
                tuple.component4(), tuple.component5());
    }

    private static void success(TransactionReceipt receipt) {
        if (receipt == null || !receipt.isStatusOK()) throw new IllegalStateException("Blockchain transaction failed");
    }

    @Override public Created createOrder(String customer, String hash) throws Exception {
        TransactionReceipt receipt = signingContract(ownerKey, true).createOrder(customer, hash).send();
        success(receipt);
        List<SupplyChainOrder.OrderCreatedEvent> events = SupplyChainOrder.getOrderCreatedEvents(receipt);
        if (events.size() != 1) throw new IllegalStateException("OrderCreated event was not found exactly once");
        var event = events.get(0);
        if (!event.customer().equalsIgnoreCase(customer) || !event.metadataHash().equalsIgnoreCase(hash) ||
                !event.orderId().equals(findByHash(hash)))
            throw new IllegalStateException("OrderCreated event does not match requested order");
        ChainOrder order = getOrder(event.orderId());
        if (!order.customer().equalsIgnoreCase(customer) || !order.metadataHash().equalsIgnoreCase(hash))
            throw new IllegalStateException("Created on-chain order does not match the buyer or metadata");
        return new Created(order, receipt.getTransactionHash());
    }

    @Override public Created recoverOrder(BigInteger id, String hash) throws Exception {
        ChainOrder order = getOrder(id);
        if (!order.metadataHash().equalsIgnoreCase(hash)) throw new IllegalStateException("On-chain metadata hash mismatch");
        EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, contractAddress);
        filter.addSingleTopic(EventEncoder.encode(SupplyChainOrder.ORDER_CREATED));
        filter.addSingleTopic(Numeric.toHexStringWithPrefixZeroPadded(id, 64));
        EthLog logs = web3j.ethGetLogs(filter).send();
        for (EthLog.LogResult<?> item : logs.getLogs()) {
            if (!(item.get() instanceof Log log) || !log.getAddress().equalsIgnoreCase(contractAddress)) continue;
            List<Type> fields = FunctionReturnDecoder.decode(log.getData(), SupplyChainOrder.ORDER_CREATED.getNonIndexedParameters());
            if (fields.size() == 2 && Numeric.toHexString((byte[]) fields.get(0).getValue()).equalsIgnoreCase(hash))
                return new Created(order, log.getTransactionHash());
        }
        throw new IllegalStateException("On-chain order exists but its creation transaction could not be recovered");
    }

    @Override public ChainOrder updateStatus(BigInteger id, int status) throws Exception {
        if (status < 1 || status > 2) throw new IllegalArgumentException("Invalid blockchain status");
        TransactionReceipt receipt = signingContract(operatorKey, false).updateOrderStatus(id, BigInteger.valueOf(status)).send();
        success(receipt);
        ChainOrder result = getOrder(id);
        if (result.status() != status) throw new IllegalStateException("Confirmed on-chain status does not match request");
        return result;
    }
}
