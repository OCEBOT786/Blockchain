import { ethers } from 'ethers';

const provider = new ethers.JsonRpcProvider(
  process.env.LOCAL_RPC_URL
);

const owner = new ethers.Wallet(
  process.env.HARDHAT_OWNER_PRIVATE_KEY,
  provider
);

const operator = new ethers.Wallet(
  process.env.HARDHAT_OPERATOR_PRIVATE_KEY,
  provider
);

const contractAddress =
  process.env.LOCAL_CONTRACT_ADDRESS;

const abi = [
  'function authorizeOperator(address _operator) external',
  'function authorizedOperators(address) external view returns (bool)',
  'function orderCount() external view returns (uint256)',
  'function updateOrderStatus(uint256 _orderId, uint8 _newStatus) external',
  'function getOrder(uint256 _orderId) external view returns (uint256 orderId, address customerAddress, uint8 status, uint256 timestamp)'
];

const ownerContract = new ethers.Contract(
  contractAddress,
  abi,
  owner
);

const operatorContract = new ethers.Contract(
  contractAddress,
  abi,
  operator
);


// authorise logistics provider wallet

console.log('Operator wallet:', operator.address);

const alreadyAuthorized =
  await ownerContract.authorizedOperators(
    operator.address
  );

if (!alreadyAuthorized) {
  const authorizeTx =
    await ownerContract.authorizeOperator(
      operator.address
    );

  console.log(
    'Authorization transaction:',
    authorizeTx.hash
  );

  await authorizeTx.wait();

  console.log('Operator authorized.');
} else {
  console.log('Operator already authorized.');
}


const orderId = process.env.TEST_BLOCKCHAIN_ORDER_ID
  ? BigInt(process.env.TEST_BLOCKCHAIN_ORDER_ID)
  : await ownerContract.orderCount();

if (orderId < 1n) {
  throw new Error('No blockchain order exists to update.');
}

const before = await operatorContract.getOrder(orderId);
console.log('Order ID:', orderId.toString());
console.log('Status before update:', Number(before.status));

// update the selected blockchain order

// solidity enum:
// 0 = OrderReceived
// 1 = InTransit
// 2 = Delivered

const updateTx =
  await operatorContract.updateOrderStatus(
    orderId,
    1
  );

console.log(
  '\nStatus update transaction:',
  updateTx.hash
);

await updateTx.wait();

console.log('Status update confirmed.');


// verify blockchain record

const order =
  await operatorContract.getOrder(orderId);

console.log('\nBlockchain order after update:');

console.log({
  orderId: order.orderId.toString(),
  customerAddress: order.customerAddress,
  status: Number(order.status),
  timestamp: order.timestamp.toString()
});

if (Number(order.status) === 1) {
  console.log(
    '\nSuccess: the order is now InTransit.'
  );
} else {
  throw new Error('The blockchain status did not change to InTransit.');
}
