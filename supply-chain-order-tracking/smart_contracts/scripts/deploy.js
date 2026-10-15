import { network } from "hardhat";

async function main() {
  const { ethers } = await network.create();
  if ((await ethers.provider.getNetwork()).chainId !== 31337n) {
    throw new Error("This deployment script is limited to the local Hardhat chain.");
  }
  const contract = await ethers.deployContract("SupplyChainOrder");
  await contract.waitForDeployment();
  const [, operator] = await ethers.getSigners();
  await (await contract.authorizeOperator(operator.address)).wait();
  console.log(`SupplyChainOrder deployed to: ${await contract.getAddress()}`);
  console.log(`Local operator authorized: ${operator.address}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
