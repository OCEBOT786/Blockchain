import { network } from "hardhat";

async function main() {
  const { ethers } = await network.create();
  if ((await ethers.provider.getNetwork()).chainId !== 31337n) {
    throw new Error("This deployment script is limited to the local Hardhat chain.");
  }
  const signers = await ethers.getSigners();
  const ownerIndex = Number(process.env.LOCAL_OWNER_ACCOUNT_INDEX ?? "0");
  if (!Number.isSafeInteger(ownerIndex) || ownerIndex < 0 || ownerIndex >= signers.length || ownerIndex === 1) {
    throw new Error("LOCAL_OWNER_ACCOUNT_INDEX must select a local account other than operator account 1.");
  }
  const owner = signers[ownerIndex];
  const factory = await ethers.getContractFactory("SupplyChainOrder", owner);
  const contract = await factory.deploy();
  await contract.waitForDeployment();
  const operator = signers[1];
  await (await contract.authorizeOperator(operator.address)).wait();
  console.log(`SupplyChainOrder deployed to: ${await contract.getAddress()}`);
  console.log(`Local owner: ${owner.address} (Hardhat account ${ownerIndex})`);
  console.log(`Local operator authorized: ${operator.address}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
