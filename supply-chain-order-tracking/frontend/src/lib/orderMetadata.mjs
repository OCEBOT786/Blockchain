import { AbiCoder, keccak256 } from 'ethers';

export const ORDER_METADATA_VERSION = 'SCOT_ORDER_METADATA_V1';
export const ORDER_METADATA_ABI_TYPES = Object.freeze([
  'string', 'bytes16', 'bytes16', 'string', 'bool', 'string',
  'uint256', 'bool', 'string'
]);

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function uuidBytes(value, field) {
  if (typeof value !== 'string' || !UUID_PATTERN.test(value)) {
    throw new TypeError(`${field} must be a UUID`);
  }
  return `0x${value.replaceAll('-', '').toLowerCase()}`;
}

function optionalString(value, field) {
  if (value === undefined) throw new TypeError(`${field} is missing`);
  if (value === null) return [false, ''];
  if (typeof value !== 'string') throw new TypeError(`${field} must be a string or null`);
  return [true, value];
}

export function encodeOrderMetadata(order) {
  if (!order || typeof order !== 'object') throw new TypeError('order must be an object');
  if (typeof order.product_name !== 'string' || order.product_name.length === 0) {
    throw new TypeError('product_name must be a nonempty string');
  }
  if (!Number.isSafeInteger(order.quantity) || order.quantity <= 0) {
    throw new TypeError('quantity must be a positive safe integer');
  }

  const [hasDescription, description] = optionalString(order.product_description, 'product_description');
  const [hasDeliveryAddress, deliveryAddress] = optionalString(order.delivery_address, 'delivery_address');

  return AbiCoder.defaultAbiCoder().encode(ORDER_METADATA_ABI_TYPES, [
    ORDER_METADATA_VERSION,
    uuidBytes(order.id, 'id'),
    uuidBytes(order.buyer_id, 'buyer_id'),
    order.product_name,
    hasDescription,
    description,
    BigInt(order.quantity),
    hasDeliveryAddress,
    deliveryAddress
  ]);
}

export function hashOrderMetadata(order) {
  return keccak256(encodeOrderMetadata(order));
}
