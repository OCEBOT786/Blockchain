import hre from "hardhat";

async function main() {
  const SupplyChainOrder = await hre.ethers.getContractFactory("SupplyChainOrder");
  const supplyChainOrder = await SupplyChainOrder.deploy();

  if (supplyChainOrder.waitForDeployment) {
    await supplyChainOrder.waitForDeployment();
  } else {
    await supplyChainOrder.deployed();
  }

  const contractAddress = supplyChainOrder.target || supplyChainOrder.address;
  console.log(`SupplyChainOrder deployed to: ${contractAddress}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
