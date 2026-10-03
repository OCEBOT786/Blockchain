import { createClient } from '@supabase/supabase-js';
import { ethers } from 'ethers';

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
  'function createOrder(address _customer) external returns (uint256)',
  'function orderCount() external view returns (uint256)',
  'function getOrder(uint256 _orderId) external view returns (uint256 orderId, address customerAddress, uint8 status, uint256 timestamp)',
  'event OrderCreated(uint256 indexed orderId, address indexed customerAddress, uint256 timestamp)'
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

console.log(
  '\nCreating blockchain order for:',
  profile.wallet_address
);


// create blockchain order

const tx = await contract.createOrder(
  profile.wallet_address
);

console.log('\nTransaction sent:');
console.log(tx.hash);

const receipt = await tx.wait();

console.log('Transaction confirmed.');


// get new blockchain order ID


const blockchainOrderId =
  await contract.orderCount();

console.log(
  'Blockchain order ID:',
  blockchainOrderId.toString()
);


// update supabase order


const { data: updatedOrder, error: updateError } =
  await supabase
    .from('orders')
    .update({
      blockchain_order_id:
        Number(blockchainOrderId),

      blockchain_tx_hash:
        receipt.hash,

      contract_address:
        contractAddress
    })
    .eq('id', order.id)
    .select()
    .single();

if (updateError) {
  console.error('Supabase update failed:');
  console.error(updateError);
  process.exit(1);
}

console.log('\nOrder linked successfully:');
console.log(updatedOrder);


// verify blockchain data


const blockchainOrder =
  await contract.getOrder(blockchainOrderId);

console.log('\nBlockchain record:');
console.log({
  orderId: blockchainOrder.orderId.toString(),
  customerAddress: blockchainOrder.customerAddress,
  status: Number(blockchainOrder.status),
  timestamp: blockchainOrder.timestamp.toString()
});