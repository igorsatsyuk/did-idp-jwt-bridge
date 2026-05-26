const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("DidRegistry", function () {
  let registry;
  let owner;
  let other;

  beforeEach(async function () {
    [owner, other] = await ethers.getSigners();
    const DidRegistry = await ethers.getContractFactory("DidRegistry");
    registry = await DidRegistry.deploy();
  });

  it("should register a DID", async function () {
    await registry.registerDid("did:example:123", "0xpublickey");
    const [publicKey, status] = await registry.getDid("did:example:123");
    expect(publicKey).to.equal("0xpublickey");
    expect(status).to.equal(0);
  });

  it("should revoke a DID", async function () {
    await registry.registerDid("did:example:456", "0xpublickey2");
    await registry.revokeDid("did:example:456");
    const [, status] = await registry.getDid("did:example:456");
    expect(status).to.equal(1);
  });

  it("should not allow duplicate registration", async function () {
    await registry.registerDid("did:example:789", "0xpublickey3");
    await expect(
      registry.registerDid("did:example:789", "0xpublickey3")
    ).to.be.revertedWith("DID already registered");
  });

  it("should only allow owner to revoke", async function () {
    await registry.registerDid("did:example:abc", "0xpublickey4");
    await expect(
      registry.connect(other).revokeDid("did:example:abc")
    ).to.be.revertedWith("Not the DID owner");
  });
});
