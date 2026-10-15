import hre from "hardhat";

async function main() {
  console.log("Deploying SupplyChainOrder contract...");
  
  const SupplyChainOrder = await hre.ethers.getContractFactory("SupplyChainOrder");
  const contract = await SupplyChainOrder.deploy();
  await contract.waitForDeployment();
  
  console.log(`SupplyChainOrder successfully deployed to: ${contract.target}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});