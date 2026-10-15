import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { network } from 'hardhat';

const HASH = `0x${'11'.repeat(32)}`;

async function deployFixture() {
  const { ethers } = await network.create();
  const [owner, operator, customer, unauthorized] = await ethers.getSigners();
  const contract = await ethers.deployContract('SupplyChainOrder');
  await contract.waitForDeployment();
  return { contract, owner, operator, customer, unauthorized };
}

describe('SupplyChainOrder', () => {
  it('assigns the deployer as owner', async () => {
    const { contract, owner } = await deployFixture();
    assert.equal(await contract.owner(), owner.address);
  });

  it('stores and emits the immutable metadata hash on creation', async () => {
    const { contract, owner, customer } = await deployFixture();
    const tx = await contract.connect(owner).createOrder(customer.address, HASH);
    const receipt = await tx.wait();
    const created = receipt.logs
      .map((log) => { try { return contract.interface.parseLog(log); } catch { return null; } })
      .find((event) => event?.name === 'OrderCreated');

    assert.equal(created.args.orderId, 1n);
    assert.equal(created.args.customerAddress, customer.address);
    assert.equal(created.args.metadataHash, HASH);
    const order = await contract.getOrder(1);
    assert.equal(order.status, 0n);
    assert.equal(order.metadataHash, HASH);
    assert.equal(await contract.orderIdByMetadataHash(HASH), 1n);
    assert.ok(order.timestamp > 0n);
  });

  it('rejects an empty hash and creation by a non-owner', async () => {
    const { contract, customer, unauthorized } = await deployFixture();
    await assert.rejects(contract.createOrder(customer.address, `0x${'00'.repeat(32)}`));
    await assert.rejects(contract.connect(unauthorized).createOrder(customer.address, HASH));
  });

  it('rejects a status update by a non-operator', async () => {
    const { contract, customer, unauthorized } = await deployFixture();
    await (await contract.createOrder(customer.address, HASH)).wait();
    await assert.rejects(contract.connect(unauthorized).updateOrderStatus(1, 1));
  });

  it('prevents duplicate blockchain records for the same metadata hash', async () => {
    const { contract, customer } = await deployFixture();
    await (await contract.createOrder(customer.address, HASH)).wait();
    await assert.rejects(contract.createOrder(customer.address, HASH));
    assert.equal(await contract.orderCount(), 1n);
  });

  it('allows an authorized operator to update status without changing the hash', async () => {
    const { contract, operator, customer } = await deployFixture();
    await (await contract.createOrder(customer.address, HASH)).wait();
    await (await contract.authorizeOperator(operator.address)).wait();
    await (await contract.connect(operator).updateOrderStatus(1, 1)).wait();
    assert.equal((await contract.getOrder(1)).status, 1n);
    await (await contract.connect(operator).updateOrderStatus(1, 2)).wait();
    const order = await contract.getOrder(1);
    assert.equal(order.status, 2n);
    assert.equal(order.metadataHash, HASH);
  });
});
