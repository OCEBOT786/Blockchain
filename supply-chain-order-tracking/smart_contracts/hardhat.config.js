import hardhatEthers from "@nomicfoundation/hardhat-ethers";

export default {
  plugins: [hardhatEthers],
  solidity: "0.8.20",
  networks: {
    hardhatMainnet: { type: "edr-simulated", chainType: "l1" },
    localhost: { type: "http", chainType: "l1", url: process.env.LOCAL_RPC_URL || "http://127.0.0.1:8545", accounts: "remote" },
  },
};
