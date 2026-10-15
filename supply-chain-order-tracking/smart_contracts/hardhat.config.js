import "@nomicfoundation/hardhat-toolbox";

export default {
  solidity: "0.8.20",
  networks: {
    sepolia: {
      url: "https://ethereum-sepolia-rpc.publicnode.com",
      accounts: ["cccd852ea856387fc50ffb6ea442bcb7baeae5e8cef22aa5357c85eacdc1d82c"] 
    }
  }
};