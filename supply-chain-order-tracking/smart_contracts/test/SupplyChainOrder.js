import { expect } from "chai";
import { ethers } from "hardhat";


describe("SupplyChainOrder", function () {
  let supplyChainOrder;
  let owner, operator, customer, unauthorized;

  beforeEach(async function () {
    [owner, operator, customer, unauthorized] = await ethers.getSigners();

    const SupplyChainOrder = await ethers.getContractFactory("SupplyChainOrder");
    supplyChainOrder = await SupplyChainOrder.deploy();

    if (supplyChainOrder.waitForDeployment) {
      await supplyChainOrder.waitForDeployment();
    } else {
      await supplyChainOrder.deployed();
    }
  });

  it("should deploy successfully and assign the correct owner", async function () {
    expect(await supplyChainOrder.owner()).to.equal(owner.address);
  });

  it("should allow owner to create an order with OrderReceived status", async function () {
    await supplyChainOrder.connect(owner).createOrder(customer.address);

    const order = await supplyChainOrder.getOrder(1);
    expect(order.orderId).to.equal(1);
    expect(order.customerAddress).to.equal(customer.address);
    expect(order.status).to.equal(0); // Status.OrderReceived
    expect(order.timestamp).to.be.gt(0);
  });

  it("should prevent unauthorized address from updating order status", async function () {
    await supplyChainOrder.connect(owner).createOrder(customer.address);

    await expect(
      supplyChainOrder.connect(unauthorized).updateOrderStatus(1, 1) // 1: InTransit
    ).to.be.revertedWith("Not an authorized operator");
  });

  it("should allow authorized operator to update status to InTransit and Delivered", async function () {
    await supplyChainOrder.connect(owner).createOrder(customer.address);
    await supplyChainOrder.connect(owner).authorizeOperator(operator.address);

    // Update to InTransit (1)
    await supplyChainOrder.connect(operator).updateOrderStatus(1, 1);
    let order = await supplyChainOrder.getOrder(1);
    expect(order.status).to.equal(1);

    // Update to Delivered (2)
    await supplyChainOrder.connect(operator).updateOrderStatus(1, 2);
    order = await supplyChainOrder.getOrder(1);
    expect(order.status).to.equal(2);
  });
});
