import { network } from "hardhat";

async function main() {
  const { ethers } = await network.create();

  const SupplyChainOrder =
    await ethers.getContractFactory("SupplyChainOrder");

  const supplyChainOrder =
    await SupplyChainOrder.deploy();

  await supplyChainOrder.waitForDeployment();

  const contractAddress =
    await supplyChainOrder.getAddress();

  console.log(
    `SupplyChainOrder deployed to: ${contractAddress}`
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});