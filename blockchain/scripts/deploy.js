async function main(runtime = require("hardhat"), logger = console) {
  logger.log("Deploying DidRegistry...");
  const DidRegistry = await runtime.ethers.getContractFactory("DidRegistry");
  const registry = await DidRegistry.deploy();
  await registry.waitForDeployment();
  const address = await registry.getAddress();
  logger.log(`DidRegistry deployed to: ${address}`);
  logger.log(`\nAdd to your .env:\nDID_REGISTRY_ADDRESS=${address}`);
  return address;
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
}

module.exports = { main };
