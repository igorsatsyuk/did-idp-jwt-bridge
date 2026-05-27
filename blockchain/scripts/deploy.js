// Thin CLI wrapper — always invoked by `hardhat run scripts/deploy.js`
const { main } = require("./deploy-main");

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
