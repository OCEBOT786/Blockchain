import { createClient } from '@supabase/supabase-js';
import { ethers } from 'ethers';
import { hashOrderMetadata } from './src/lib/orderMetadata.mjs';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL,
  process.env.VITE_SUPABASE_PUBLISHABLE_KEY
);

const provider = new ethers.JsonRpcProvider(
  process.env.LOCAL_RPC_URL
);

const owner = new ethers.Wallet(
  process.env.HARDHAT_OWNER_PRIVATE_KEY,
  provider
);

const contractAddress =
  process.env.LOCAL_CONTRACT_ADDRESS;


const abi = [
  'function createOrder(address _customer, bytes32 _metadataHash) external returns (uint256)',
  'function orderIdByMetadataHash(bytes32) external view returns (uint256)',
  'function getOrder(uint256 _orderId) external view returns (uint256 orderId, address customerAddress, uint8 status, uint256 timestamp, bytes32 metadataHash)',
  'event OrderCreated(uint256 indexed orderId, address indexed customerAddress, bytes32 metadataHash, uint256 timestamp)'
];

const contract = new ethers.Contract(
  contractAddress,
  abi,
  owner
);


// login to supabase

const { data: loginData, error: loginError } =
  await supabase.auth.signInWithPassword({
    email: process.env.TEST_EMAIL,
    password: process.env.TEST_PASSWORD
  });

if (loginError) {
  console.error('Supabase login failed:', loginError);
  process.exit(1);
}

const user = loginData.user;

console.log('Logged in as:', user.email);


// get latest unlinked order for this buyer

const { data: order, error: orderError } =
  await supabase
    .from('orders')
    .select('*')
    .eq('buyer_id', user.id)
    .is('blockchain_order_id', null)
    .order('created_at', { ascending: false })
    .limit(1)
    .single();

if (orderError) {
  console.error('Could not retrieve Supabase order:');
  console.error(orderError);
  process.exit(1);
}

console.log('\nSupabase order found:');
console.log(order);


// get buyer wallet address

const { data: profile, error: profileError } =
  await supabase
    .from('profiles')
    .select('wallet_address')
    .eq('id', user.id)
    .single();

if (profileError) {
  console.error('Could not retrieve buyer profile:');
  console.error(profileError);
  process.exit(1);
}

if (!profile.wallet_address || !ethers.isAddress(profile.wallet_address)) {
  throw new Error('The buyer profile needs a valid wallet address before blockchain linking.');
}

const metadataHash = hashOrderMetadata(order);
let blockchainOrderId = await contract.orderIdByMetadataHash(metadataHash);
let transactionHash;
let createdEvent;

if (blockchainOrderId > 0n) {
  // Recover a confirmed chain record after an earlier Supabase-link failure.
  const events = await contract.queryFilter(contract.filters.OrderCreated(blockchainOrderId));
  if (events.length !== 1) throw new Error('Could not uniquely recover OrderCreated.');
  createdEvent = events[0];
  transactionHash = createdEvent.transactionHash;
  console.log('Reusing existing blockchain order:', blockchainOrderId.toString());
} else {
  const tx = await contract.createOrder(profile.wallet_address, metadataHash);
  console.log('\nTransaction sent:', tx.hash);
  const receipt = await tx.wait();
  if (!receipt || receipt.status !== 1) {
    throw new Error('The blockchain transaction did not succeed.');
  }
  createdEvent = receipt.logs
    .filter((log) => log.address.toLowerCase() === contractAddress.toLowerCase())
    .map((log) => {
      try { return contract.interface.parseLog(log); } catch { return null; }
    })
    .find((event) => event?.name === 'OrderCreated');
  if (!createdEvent) throw new Error('The transaction did not emit OrderCreated.');
  blockchainOrderId = createdEvent.args.orderId;
  transactionHash = receipt.hash;
}

const numericOrderId = Number(blockchainOrderId);

if (!Number.isSafeInteger(numericOrderId)) {
  throw new Error('Blockchain order ID exceeds the safe JavaScript integer range.');
}

const blockchainOrder = await contract.getOrder(blockchainOrderId);

if (
  blockchainOrder.orderId !== blockchainOrderId ||
  blockchainOrder.customerAddress.toLowerCase() !== profile.wallet_address.toLowerCase() ||
  createdEvent.args.customerAddress.toLowerCase() !== profile.wallet_address.toLowerCase() ||
  createdEvent.args.metadataHash.toLowerCase() !== metadataHash.toLowerCase() ||
  blockchainOrder.metadataHash.toLowerCase() !== metadataHash.toLowerCase()
) {
  throw new Error('The blockchain order does not match the buyer profile and metadata hash.');
}

console.log(
  'Blockchain order ID:',
  blockchainOrderId.toString()
);


// update supabase order

// An order can change while its blockchain transaction is pending. Do not link
// that immutable hash to a different version of the protected metadata.
const { data: latestOrder, error: latestOrderError } = await supabase
  .from('orders')
  .select('id, buyer_id, product_name, product_description, quantity, delivery_address')
  .eq('id', order.id)
  .single();
if (latestOrderError || hashOrderMetadata(latestOrder) !== metadataHash) {
  throw new Error('Order metadata changed before linking; manual reconciliation is required.');
}


let linkQuery = supabase
  .from('orders')
  .update({
    blockchain_order_id: numericOrderId,
    blockchain_tx_hash: transactionHash,
    contract_address: contractAddress
  })
  .eq('id', order.id)
  .eq('buyer_id', order.buyer_id)
  .eq('product_name', order.product_name)
  .eq('quantity', order.quantity)
  .is('blockchain_order_id', null);
linkQuery = order.product_description === null
  ? linkQuery.is('product_description', null)
  : linkQuery.eq('product_description', order.product_description);
linkQuery = order.delivery_address === null
  ? linkQuery.is('delivery_address', null)
  : linkQuery.eq('delivery_address', order.delivery_address);
const { data: updatedOrder, error: updateError } = await linkQuery.select().single();

if (updateError) {
  console.error('Supabase update failed:');
  console.error(updateError);
  process.exit(1);
}

if (
  BigInt(updatedOrder.blockchain_order_id) !== blockchainOrderId ||
  updatedOrder.blockchain_tx_hash?.toLowerCase() !== transactionHash.toLowerCase() ||
  updatedOrder.contract_address?.toLowerCase() !== contractAddress.toLowerCase()
) {
  throw new Error('The saved Supabase link does not match the transaction.');
}

console.log('\nOrder linked successfully:');
console.log(updatedOrder);


// verify blockchain data


console.log('\nBlockchain record:');
console.log({
  orderId: blockchainOrder.orderId.toString(),
  customerAddress: blockchainOrder.customerAddress,
  status: Number(blockchainOrder.status),
  timestamp: blockchainOrder.timestamp.toString(),
  metadataHash: blockchainOrder.metadataHash
});
