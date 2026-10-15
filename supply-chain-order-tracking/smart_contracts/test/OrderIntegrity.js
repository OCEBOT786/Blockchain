import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { network } from 'hardhat';
import {
  hashOrderMetadata,
  encodeOrderMetadata
} from '../../frontend/src/lib/orderMetadata.mjs';
import {
  verifyOrderIntegrity,
  VERIFICATION_STATUS
} from '../../frontend/src/lib/verifyOrderIntegrity.mjs';

const ORDER = Object.freeze({
  id: '12345678-1234-4234-8234-123456789abc',
  buyer_id: '87654321-4321-4321-8321-cba987654321',
  product_name: 'Coffee Beans',
  product_description: 'Premium coffee',
  quantity: 20,
  delivery_address: '123 Test Street, Auckland',
  notes: 'Unprotected note',
  wholesaler_id: null,
  logistics_provider_id: null
});

function mockSupabase(row, error = null) {
  return {
    from(table) {
      assert.equal(table, 'orders');
      return {
        select() {
          return {
            eq(column, id) {
              assert.equal(column, 'id');
              assert.equal(id, ORDER.id);
              return { async single() { return { data: row, error }; } };
            }
          };
        }
      };
    }
  };
}

describe('versioned order metadata hash', () => {
  it('is deterministic and restores exactly after a change', () => {
    const original = hashOrderMetadata(ORDER);
    assert.match(original, /^0x[0-9a-f]{64}$/);
    assert.equal(original, '0x950baa1d77b7153a7753d89cfa5ed90404e79acca9b4f0a56aeee74c43ccc3b9');
    assert.equal(hashOrderMetadata({ ...ORDER }), original);
    const changed = { ...ORDER, product_name: 'Tea' };
    assert.notEqual(hashOrderMetadata(changed), original);
    assert.equal(hashOrderMetadata({ ...changed, product_name: ORDER.product_name }), original);
    assert.equal(encodeOrderMetadata({ ...ORDER }), encodeOrderMetadata(ORDER));
  });

  it('changes when any protected field changes', () => {
    const original = hashOrderMetadata(ORDER);
    const changes = {
      id: '22345678-1234-4234-8234-123456789abc',
      buyer_id: '97654321-4321-4321-8321-cba987654321',
      product_name: 'Tea',
      product_description: 'Different description',
      quantity: 21,
      delivery_address: 'Different address'
    };
    for (const [field, value] of Object.entries(changes)) {
      assert.notEqual(hashOrderMetadata({ ...ORDER, [field]: value }), original, field);
    }
  });

  it('distinguishes null from empty optional values and ignores mutable fields', () => {
    assert.notEqual(hashOrderMetadata({ ...ORDER, product_description: null }),
      hashOrderMetadata({ ...ORDER, product_description: '' }));
    assert.notEqual(hashOrderMetadata({ ...ORDER, delivery_address: null }),
      hashOrderMetadata({ ...ORDER, delivery_address: '' }));
    assert.equal(hashOrderMetadata({ ...ORDER, notes: 'Changed', logistics_provider_id: ORDER.buyer_id }),
      hashOrderMetadata(ORDER));
    assert.throws(() => hashOrderMetadata({ ...ORDER, product_description: undefined }));
  });
});

describe('local blockchain integrity verification', () => {
  it('returns VERIFIED for the anchored order and MISMATCH for changed metadata', async () => {
    const { ethers } = await network.create();
    const [buyer] = await ethers.getSigners();
    const contract = await ethers.deployContract('SupplyChainOrder');
    await contract.waitForDeployment();
    const tx = await contract.createOrder(buyer.address, hashOrderMetadata(ORDER));
    const receipt = await tx.wait();
    const linked = {
      ...ORDER,
      blockchain_order_id: 1,
      blockchain_tx_hash: receipt.hash,
      contract_address: await contract.getAddress()
    };
    const trust = {
      trustedContractAddress: linked.contract_address,
      trustedChainId: (await ethers.provider.getNetwork()).chainId
    };

    const verified = await verifyOrderIntegrity({
      supabase: mockSupabase(linked), provider: ethers.provider, orderId: ORDER.id, ...trust
    });
    assert.equal(verified.status, VERIFICATION_STATUS.VERIFIED, verified.message);
    assert.equal(verified.storedHash, hashOrderMetadata(ORDER));

    const mismatch = await verifyOrderIntegrity({
      supabase: mockSupabase({ ...linked, quantity: 21 }),
      provider: ethers.provider,
      orderId: ORDER.id,
      ...trust
    });
    assert.equal(mismatch.status, VERIFICATION_STATUS.MISMATCH, mismatch.message);
    assert.notEqual(mismatch.currentHash, mismatch.storedHash);

    const wrongContract = await verifyOrderIntegrity({
      supabase: mockSupabase(linked), provider: ethers.provider, orderId: ORDER.id,
      ...trust, trustedContractAddress: '0x0000000000000000000000000000000000000001'
    });
    assert.equal(wrongContract.status, VERIFICATION_STATUS.VERIFICATION_ERROR);

    const wrongNetwork = await verifyOrderIntegrity({
      supabase: mockSupabase(linked), provider: ethers.provider, orderId: ORDER.id,
      ...trust, trustedChainId: trust.trustedChainId + 1n
    });
    assert.equal(wrongNetwork.status, VERIFICATION_STATUS.VERIFICATION_ERROR);
  });

  it('never verifies an unlinked or unreadable order', async () => {
    const { ethers } = await network.create();
    const trust = {
      trustedContractAddress: '0x0000000000000000000000000000000000000001',
      trustedChainId: (await ethers.provider.getNetwork()).chainId
    };
    const unlinked = await verifyOrderIntegrity({
      supabase: mockSupabase({ ...ORDER, blockchain_order_id: null, blockchain_tx_hash: null, contract_address: null }),
      provider: ethers.provider,
      orderId: ORDER.id,
      ...trust
    });
    assert.equal(unlinked.status, VERIFICATION_STATUS.NOT_LINKED);

    const unreadable = await verifyOrderIntegrity({
      supabase: mockSupabase(null, { message: 'Access denied' }),
      provider: ethers.provider,
      orderId: ORDER.id,
      ...trust
    });
    assert.equal(unreadable.status, VERIFICATION_STATUS.VERIFICATION_ERROR);

    const placeholder = await verifyOrderIntegrity({
      supabase: mockSupabase({
        ...ORDER,
        blockchain_order_id: 1,
        blockchain_tx_hash: 'pending',
        contract_address: '0x0000000000000000000000000000000000000001'
      }),
      provider: ethers.provider,
      orderId: ORDER.id,
      ...trust
    });
    assert.equal(placeholder.status, VERIFICATION_STATUS.VERIFICATION_ERROR);
  });
});
