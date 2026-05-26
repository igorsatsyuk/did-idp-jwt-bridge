const hre = require("hardhat");

async function main() {
  console.log("Deploying DidRegistry...");
  const DidRegistry = await hre.ethers.getContractFactory("DidRegistry");
  const registry = await DidRegistry.deploy();
  await registry.waitForDeployment();
  const address = await registry.getAddress();
  console.log(`DidRegistry deployed to: ${address}`);
  console.log(`\nAdd to your .env:\nDID_REGISTRY_ADDRESS=${address}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
