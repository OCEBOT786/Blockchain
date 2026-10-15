package com.supplychain.contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

/** ABI adapter for the metadata-integrity SupplyChainOrder contract. */
public class SupplyChainOrder extends Contract {
    public static final Event ORDER_CREATED = new Event("OrderCreated", Arrays.asList(
            new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {},
            new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    public static final Event STATUS_UPDATED = new Event("OrderStatusUpdated", Arrays.asList(
            new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {},
            new TypeReference<Uint256>() {}));

    private SupplyChainOrder(String address, Web3j web3j, Credentials credentials, ContractGasProvider gas) {
        super("0x", address, web3j, credentials, gas);
    }

    public static SupplyChainOrder load(String address, Web3j web3j, Credentials credentials, ContractGasProvider gas) {
        return new SupplyChainOrder(address, web3j, credentials, gas);
    }

    private static Bytes32 bytes32(String hex) {
        byte[] value = Numeric.hexStringToByteArray(hex);
        if (value.length != 32) throw new IllegalArgumentException("Expected bytes32");
        return new Bytes32(value);
    }

    public RemoteFunctionCall<TransactionReceipt> createOrder(String customer, String hash) {
        Function function = new Function("createOrder", Arrays.<Type>asList(new Address(customer), bytes32(hash)), Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> authorizeOperator(String wallet) {
        Function function = new Function("authorizeOperator", Arrays.<Type>asList(new Address(wallet)), Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateOrderStatus(BigInteger id, BigInteger status) {
        Function function = new Function("updateOrderStatus", Arrays.<Type>asList(new Uint256(id), new Uint8(status)), Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> orderIdByMetadataHash(String hash) {
        Function function = new Function("orderIdByMetadataHash", Arrays.<Type>asList(bytes32(hash)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> owner() {
        Function function = new Function("owner", Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> authorizedOperators(String wallet) {
        Function function = new Function("authorizedOperators", Arrays.<Type>asList(new Address(wallet)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Tuple5<BigInteger, String, BigInteger, BigInteger, String>> getOrder(BigInteger id) {
        Function function = new Function("getOrder", Arrays.<Type>asList(new Uint256(id)), Arrays.<TypeReference<?>>asList(
                new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Uint8>() {},
                new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}));
        return new RemoteFunctionCall<>(function, () -> {
            List<Type> values = executeCallMultipleValueReturn(function);
            return new Tuple5<>((BigInteger) values.get(0).getValue(), (String) values.get(1).getValue(),
                    (BigInteger) values.get(2).getValue(), (BigInteger) values.get(3).getValue(),
                    Numeric.toHexString((byte[]) values.get(4).getValue()));
        });
    }

    public static List<OrderCreatedEvent> getOrderCreatedEvents(TransactionReceipt receipt) {
        List<OrderCreatedEvent> events = new ArrayList<>();
        for (Contract.EventValuesWithLog values : staticExtractEventParametersWithLog(ORDER_CREATED, receipt)) {
            events.add(new OrderCreatedEvent((BigInteger) values.getIndexedValues().get(0).getValue(),
                    (String) values.getIndexedValues().get(1).getValue(),
                    Numeric.toHexString((byte[]) values.getNonIndexedValues().get(0).getValue()),
                    (BigInteger) values.getNonIndexedValues().get(1).getValue()));
        }
        return events;
    }

    public record OrderCreatedEvent(BigInteger orderId, String customer, String metadataHash, BigInteger timestamp) {}
}
