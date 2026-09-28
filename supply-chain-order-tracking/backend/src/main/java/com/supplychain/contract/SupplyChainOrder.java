package com.supplychain.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Generated;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.8.0.
 */
@SuppressWarnings("rawtypes")
@Generated("org.web3j.codegen.SolidityFunctionWrapperGenerator")
public class SupplyChainOrder extends Contract {
    public static final String BINARY = "0x608060405234801561000f575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550610e5f8061005c5f395ff3fe608060405234801561000f575f80fd5b5060043610610086575f3560e01c8063a85c38ef11610059578063a85c38ef14610112578063aab3196d14610145578063d09ef24114610161578063ea3774e31461019457610086565b8063181d989b1461008a5780632453ffa8146100ba5780638da5cb5b146100d8578063959b8c3f146100f6575b5f80fd5b6100a4600480360381019061009f9190610953565b6101c4565b6040516100b19190610998565b60405180910390f35b6100c26101e1565b6040516100cf91906109c9565b60405180910390f35b6100e06101e7565b6040516100ed91906109f1565b60405180910390f35b610110600480360381019061010b9190610953565b61020a565b005b61012c60048036038101906101279190610a34565b6103a0565b60405161013c9493929190610ad2565b60405180910390f35b61015f600480360381019061015a9190610b38565b6103f8565b005b61017b60048036038101906101769190610a34565b610564565b60405161018b9493929190610ad2565b60405180910390f35b6101ae60048036038101906101a99190610953565b61069d565b6040516101bb91906109c9565b60405180910390f35b6003602052805f5260405f205f915054906101000a900460ff1681565b60015481565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610297576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161028e90610bd0565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610305576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102fc90610c38565b60405180910390fd5b600160035f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167fcc13c9db8dd1ea1e88548833406d061f3c0cdddb6f94660374e5515d0b95ce0960405160405180910390a250565b6002602052805f5260405f205f91509050805f015490806001015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060010160149054906101000a900460ff16908060020154905084565b60035f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff16610481576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161047890610ca0565b60405180910390fd5b5f8211801561049257506001548211155b6104d1576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104c890610d08565b60405180910390fd5b8060025f8481526020019081526020015f2060010160146101000a81548160ff0219169083600281111561050857610507610a5f565b5b02179055504260025f8481526020019081526020015f2060020181905550817f3018f5631b85cb4b5b9aa9ef3efcc97db1d01e5e12d98808f8fba7f0f9e1dbf88242604051610558929190610d26565b60405180910390a25050565b5f805f805f8511801561057957506001548511155b6105b8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105af90610d08565b60405180910390fd5b5f60025f8781526020019081526020015f206040518060800160405290815f8201548152602001600182015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016001820160149054906101000a900460ff16600281111561065857610657610a5f565b5b600281111561066a57610669610a5f565b5b81526020016002820154815250509050805f01518160200151826040015183606001519450945094509450509193509193565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461072c576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161072390610bd0565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff160361079a576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161079190610d97565b60405180910390fd5b60015f8154809291906107ac90610de2565b9190505550604051806080016040528060015481526020018373ffffffffffffffffffffffffffffffffffffffff1681526020015f60028111156107f3576107f2610a5f565b5b81526020014281525060025f60015481526020019081526020015f205f820151815f01556020820151816001015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060408201518160010160146101000a81548160ff0219169083600281111561088857610887610a5f565b5b0217905550606082015181600201559050508173ffffffffffffffffffffffffffffffffffffffff166001547f28eb86fd03c9bd96a8a83e4a46ab2ad257b70cea893d98ad6d482e96aa25fad0426040516108e391906109c9565b60405180910390a36001549050919050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610922826108f9565b9050919050565b61093281610918565b811461093c575f80fd5b50565b5f8135905061094d81610929565b92915050565b5f60208284031215610968576109676108f5565b5b5f6109758482850161093f565b91505092915050565b5f8115159050919050565b6109928161097e565b82525050565b5f6020820190506109ab5f830184610989565b92915050565b5f819050919050565b6109c3816109b1565b82525050565b5f6020820190506109dc5f8301846109ba565b92915050565b6109eb81610918565b82525050565b5f602082019050610a045f8301846109e2565b92915050565b610a13816109b1565b8114610a1d575f80fd5b50565b5f81359050610a2e81610a0a565b92915050565b5f60208284031215610a4957610a486108f5565b5b5f610a5684828501610a20565b91505092915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602160045260245ffd5b60038110610a9d57610a9c610a5f565b5b50565b5f819050610aad82610a8c565b919050565b5f610abc82610aa0565b9050919050565b610acc81610ab2565b82525050565b5f608082019050610ae55f8301876109ba565b610af260208301866109e2565b610aff6040830185610ac3565b610b0c60608301846109ba565b95945050505050565b60038110610b21575f80fd5b50565b5f81359050610b3281610b15565b92915050565b5f8060408385031215610b4e57610b4d6108f5565b5b5f610b5b85828601610a20565b9250506020610b6c85828601610b24565b9150509250929050565b5f82825260208201905092915050565b7f4f6e6c79206f776e65722063616e2063616c6c207468697300000000000000005f82015250565b5f610bba601883610b76565b9150610bc582610b86565b602082019050919050565b5f6020820190508181035f830152610be781610bae565b9050919050565b7f496e76616c6964206f70657261746f72206164647265737300000000000000005f82015250565b5f610c22601883610b76565b9150610c2d82610bee565b602082019050919050565b5f6020820190508181035f830152610c4f81610c16565b9050919050565b7f4e6f7420616e20617574686f72697a6564206f70657261746f720000000000005f82015250565b5f610c8a601a83610b76565b9150610c9582610c56565b602082019050919050565b5f6020820190508181035f830152610cb781610c7e565b9050919050565b7f4f7264657220646f6573206e6f742065786973740000000000000000000000005f82015250565b5f610cf2601483610b76565b9150610cfd82610cbe565b602082019050919050565b5f6020820190508181035f830152610d1f81610ce6565b9050919050565b5f604082019050610d395f830185610ac3565b610d4660208301846109ba565b9392505050565b7f496e76616c696420637573746f6d6572206164647265737300000000000000005f82015250565b5f610d81601883610b76565b9150610d8c82610d4d565b602082019050919050565b5f6020820190508181035f830152610dae81610d75565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f610dec826109b1565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8203610e1e57610e1d610db5565b5b60018201905091905056fea2646970667358221220b40ec973b81f38e168bfa07ea8386520a21d6191e6ad72b91f9140dd3d55daa764736f6c63430008140033\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_AUTHORIZEOPERATOR = "authorizeOperator";

    public static final String FUNC_AUTHORIZEDOPERATORS = "authorizedOperators";

    public static final String FUNC_CREATEORDER = "createOrder";

    public static final String FUNC_GETORDER = "getOrder";

    public static final String FUNC_ORDERCOUNT = "orderCount";

    public static final String FUNC_ORDERS = "orders";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_UPDATEORDERSTATUS = "updateOrderStatus";

    public static final Event OPERATORAUTHORIZED_EVENT = new Event("OperatorAuthorized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event ORDERCREATED_EVENT = new Event("OrderCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ORDERSTATUSUPDATED_EVENT = new Event("OrderStatusUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected SupplyChainOrder(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SupplyChainOrder(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SupplyChainOrder(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SupplyChainOrder(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<OperatorAuthorizedEventResponse> getOperatorAuthorizedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OPERATORAUTHORIZED_EVENT, transactionReceipt);
        ArrayList<OperatorAuthorizedEventResponse> responses = new ArrayList<OperatorAuthorizedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OperatorAuthorizedEventResponse typedResponse = new OperatorAuthorizedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.operator = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OperatorAuthorizedEventResponse getOperatorAuthorizedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OPERATORAUTHORIZED_EVENT, log);
        OperatorAuthorizedEventResponse typedResponse = new OperatorAuthorizedEventResponse();
        typedResponse.log = log;
        typedResponse.operator = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OperatorAuthorizedEventResponse> operatorAuthorizedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOperatorAuthorizedEventFromLog(log));
    }

    public Flowable<OperatorAuthorizedEventResponse> operatorAuthorizedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OPERATORAUTHORIZED_EVENT));
        return operatorAuthorizedEventFlowable(filter);
    }

    public static List<OrderCreatedEventResponse> getOrderCreatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERCREATED_EVENT, transactionReceipt);
        ArrayList<OrderCreatedEventResponse> responses = new ArrayList<OrderCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderCreatedEventResponse typedResponse = new OrderCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.customerAddress = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderCreatedEventResponse getOrderCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERCREATED_EVENT, log);
        OrderCreatedEventResponse typedResponse = new OrderCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.customerAddress = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderCreatedEventResponse> orderCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderCreatedEventFromLog(log));
    }

    public Flowable<OrderCreatedEventResponse> orderCreatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERCREATED_EVENT));
        return orderCreatedEventFlowable(filter);
    }

    public static List<OrderStatusUpdatedEventResponse> getOrderStatusUpdatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERSTATUSUPDATED_EVENT, transactionReceipt);
        ArrayList<OrderStatusUpdatedEventResponse> responses = new ArrayList<OrderStatusUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderStatusUpdatedEventResponse typedResponse = new OrderStatusUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newStatus = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderStatusUpdatedEventResponse getOrderStatusUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERSTATUSUPDATED_EVENT, log);
        OrderStatusUpdatedEventResponse typedResponse = new OrderStatusUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.orderId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.newStatus = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OrderStatusUpdatedEventResponse> orderStatusUpdatedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderStatusUpdatedEventFromLog(log));
    }

    public Flowable<OrderStatusUpdatedEventResponse> orderStatusUpdatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERSTATUSUPDATED_EVENT));
        return orderStatusUpdatedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> authorizeOperator(String _operator) {
        final Function function = new Function(
                FUNC_AUTHORIZEOPERATOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _operator)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> authorizedOperators(String param0) {
        final Function function = new Function(FUNC_AUTHORIZEDOPERATORS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> createOrder(String _customer) {
        final Function function = new Function(
                FUNC_CREATEORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _customer)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple4<BigInteger, String, BigInteger, BigInteger>> getOrder(
            BigInteger _orderId) {
        final Function function = new Function(FUNC_GETORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_orderId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Uint8>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple4<BigInteger, String, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, String, BigInteger, BigInteger> call() throws
                            Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, String, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> orderCount() {
        final Function function = new Function(FUNC_ORDERCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple4<BigInteger, String, BigInteger, BigInteger>> orders(
            BigInteger param0) {
        final Function function = new Function(FUNC_ORDERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Uint8>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple4<BigInteger, String, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, String, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, String, BigInteger, BigInteger> call() throws
                            Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, String, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> updateOrderStatus(BigInteger _orderId,
            BigInteger _newStatus) {
        final Function function = new Function(
                FUNC_UPDATEORDERSTATUS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_orderId), 
                new org.web3j.abi.datatypes.generated.Uint8(_newStatus)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static SupplyChainOrder load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SupplyChainOrder(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SupplyChainOrder load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SupplyChainOrder(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SupplyChainOrder load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SupplyChainOrder(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SupplyChainOrder load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SupplyChainOrder(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SupplyChainOrder> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SupplyChainOrder.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    public static RemoteCall<SupplyChainOrder> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SupplyChainOrder.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<SupplyChainOrder> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SupplyChainOrder.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<SupplyChainOrder> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SupplyChainOrder.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
    }
    private static String getDeploymentBinary() {
        return BINARY;
    }

    public static class OperatorAuthorizedEventResponse extends BaseEventResponse {
        public String operator;
    }
    
    public static class OrderCreatedEventResponse extends BaseEventResponse {
        public BigInteger orderId;

        public String customerAddress;

        public BigInteger timestamp;
    }

    public static class OrderStatusUpdatedEventResponse extends BaseEventResponse {
        public BigInteger orderId;

        public BigInteger newStatus;

        public BigInteger timestamp;
    }
}
