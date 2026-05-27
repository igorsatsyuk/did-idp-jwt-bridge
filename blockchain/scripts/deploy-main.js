/**
 * Injectable deploy logic — import this module in tests.
 * @param {object} runtime  Hardhat runtime (defaults to require("hardhat"))
 * @param {object} logger   Logger (defaults to console)
 * @returns {Promise<string>} Deployed contract address
 */
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

module.exports = { main };

