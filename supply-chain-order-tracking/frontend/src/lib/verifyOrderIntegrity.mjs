import { Contract, isAddress, isHexString } from 'ethers';
import { hashOrderMetadata } from './orderMetadata.mjs';

export const VERIFICATION_STATUS = Object.freeze({
  VERIFIED: 'VERIFIED',
  MISMATCH: 'MISMATCH',
  NOT_LINKED: 'NOT_LINKED',
  VERIFICATION_ERROR: 'VERIFICATION_ERROR'
});

export const ORDER_INTEGRITY_READ_ABI = [
  'function getOrder(uint256 _orderId) view returns (uint256 orderId, address customerAddress, uint8 status, uint256 timestamp, bytes32 metadataHash)'
];

const ORDER_COLUMNS = [
  'id', 'buyer_id', 'product_name', 'product_description', 'quantity',
  'delivery_address', 'blockchain_order_id', 'blockchain_tx_hash', 'contract_address'
].join(', ');

function errorResult(message) {
  return { status: VERIFICATION_STATUS.VERIFICATION_ERROR, message };
}

export async function verifyOrderIntegrity({
  supabase, provider, orderId, trustedContractAddress, trustedChainId
}) {
  if (!supabase || !provider || !orderId || !isAddress(trustedContractAddress || '')) {
    return errorResult('Supabase client, blockchain provider, order UUID and trusted contract address are required.');
  }

  try {
    const expectedChainId = BigInt(trustedChainId);
    const network = await provider.getNetwork();
    if (expectedChainId <= 0n || network.chainId !== expectedChainId) {
      return errorResult('Blockchain provider is connected to the wrong network.');
    }

    const { data: order, error } = await supabase
      .from('orders')
      .select(ORDER_COLUMNS)
      .eq('id', orderId)
      .single();

    if (error || !order) return errorResult(error?.message || 'Order could not be read.');

    const references = [order.blockchain_order_id, order.blockchain_tx_hash, order.contract_address];
    if (references.every((value) => value === null || value === undefined)) {
      return { status: VERIFICATION_STATUS.NOT_LINKED, orderId: order.id };
    }
    if (references.some((value) => value === null || value === undefined)) {
      return errorResult('Blockchain reference fields are incomplete.');
    }

    const chainOrderId = BigInt(order.blockchain_order_id);
    if (chainOrderId <= 0n || !isAddress(order.contract_address) ||
        order.contract_address.toLowerCase() !== trustedContractAddress.toLowerCase() ||
        !isHexString(order.blockchain_tx_hash, 32)) {
      return errorResult('Blockchain reference fields are invalid.');
    }

    const currentHash = hashOrderMetadata(order);
    const contract = new Contract(order.contract_address, ORDER_INTEGRITY_READ_ABI, provider);
    const chainOrder = await contract.getOrder(chainOrderId);
    if (chainOrder.orderId !== chainOrderId) {
      return errorResult('Blockchain order ID does not match the Supabase reference.');
    }

    const storedHash = chainOrder.metadataHash;
    const details = {
      orderId: order.id,
      blockchainOrderId: chainOrderId.toString(),
      contractAddress: order.contract_address,
      transactionHash: order.blockchain_tx_hash,
      currentHash,
      storedHash,
      onchainStatus: Number(chainOrder.status)
    };
    return {
      status: currentHash.toLowerCase() === storedHash.toLowerCase()
        ? VERIFICATION_STATUS.VERIFIED
        : VERIFICATION_STATUS.MISMATCH,
      ...details
    };
  } catch (error) {
    return errorResult(error instanceof Error ? error.message : String(error));
  }
}
